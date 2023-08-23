package net.gudenau.cavegame.renderer.vk;

import net.gudenau.cavegame.logger.Logger;
import net.gudenau.cavegame.renderer.Renderer;
import net.gudenau.cavegame.renderer.Shader;
import net.gudenau.cavegame.renderer.ShaderMeta;
import net.gudenau.cavegame.renderer.ShaderType;
import net.gudenau.cavegame.resource.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.gudenau.cavegame.resource.Identifier.CAVEGAME_NAMESPACE;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public final class VkRenderer implements Renderer {
    private static final Logger LOGGER = Logger.forName("Vulkan");

    private static final int MAX_FRAMES_IN_FLIGHT = 2;
    private int currentFrame = 0;
    private boolean framebufferResized = false;

    @NotNull
    private final VulkanInstance instance;
    @Nullable
    private final VulkanDebugMessenger debugMessenger;
    @NotNull
    private final VulkanSurface surface;
    @NotNull
    private final VulkanPhysicalDevice physicalDevice;
    @NotNull
    private final VulkanLogicalDevice logicalDevice;
    @NotNull
    private VulkanSwapchain swapchain;
    @NotNull
    private List<@NotNull VulkanImageView> imageViews;
    @NotNull
    private final VulkanRenderPass renderPass;
    /*
    @NotNull
    private final VulkanGraphicsPipeline graphicsPipeline;
     */
    @NotNull
    private List<@NotNull VulkanFramebuffer> swapchainFramebuffers;
    @NotNull
    private final VulkanCommandPool commandPool;
    private final List<FrameState> frameState;

    private record FrameState(
        int index,
        @NotNull VulkanCommandBuffer commandBuffer,
        @NotNull VulkanSemaphore imageAvailableSemaphore,
        @NotNull VulkanSemaphore renderFinishedSemaphore,
        @NotNull VulkanFence inFlightFence
    ) implements AutoCloseable {
        private FrameState(int index, @NotNull VulkanLogicalDevice device, @NotNull VulkanCommandPool commandPool) {
            this(
                index,
                new VulkanCommandBuffer(device, commandPool),
                new VulkanSemaphore(device),
                new VulkanSemaphore(device),
                new VulkanFence(device)
            );
        }

        @Override
        public void close() {
            commandBuffer.close();
            imageAvailableSemaphore.close();
            renderFinishedSemaphore.close();
            inFlightFence.close();
        }
    }

    public VkRenderer(@NotNull VkWindow window) {
        try(var stack = MemoryStack.stackPush()) {
            window.resizeCallback((wind, width, height) -> framebufferResized = true);

            instance = new VulkanInstance();
            debugMessenger = VulkanUtils.ENABLE_DEBUG ? new VulkanDebugMessenger(instance) : null;
            surface = new VulkanSurface(window, instance);
            physicalDevice = VulkanPhysicalDevice.pick(instance, surface, window);
            var extent = physicalDevice.surfaceExtent(stack);
            logicalDevice = new VulkanLogicalDevice(instance, physicalDevice);
            swapchain = new VulkanSwapchain(physicalDevice, surface, logicalDevice, extent);
            imageViews = swapchain.stream()
                .mapToObj((image) -> new VulkanImageView(logicalDevice, image, swapchain.imageFormat()))
                .toList();
            renderPass = new VulkanRenderPass(logicalDevice, swapchain);

            swapchainFramebuffers = imageViews.stream()
                .map((view) -> new VulkanFramebuffer(logicalDevice, swapchain, renderPass, view, extent))
                .toList();
            commandPool = new VulkanCommandPool(physicalDevice, logicalDevice);

            frameState = IntStream.range(0, MAX_FRAMES_IN_FLIGHT)
                .mapToObj((index) -> new FrameState(index, logicalDevice, commandPool))
                .toList();

            /*
            var shaderId = new Identifier(CAVEGAME_NAMESPACE, "basic");
            try (
                var vertexShader = new VulkanShaderModule(logicalDevice, VulkanShaderModule.Type.VERTEX, shaderId);
                var fragmentShader = new VulkanShaderModule(logicalDevice, VulkanShaderModule.Type.FRAGMENT, shaderId)
            ) {
                graphicsPipeline = new VulkanGraphicsPipeline(logicalDevice, extent, renderPass, vertexShader, fragmentShader);
            }
             */
        }
    }

    private void destroySwapChain() {
        swapchainFramebuffers.forEach(VulkanFramebuffer::close);
        imageViews.forEach(VulkanImageView::close);
        swapchain.close();
    }

    private void recreateSwapChain() {
        try(var stack = MemoryStack.stackPush()) {
            logicalDevice.waitForIdle();

            destroySwapChain();

            var extent = physicalDevice.surfaceExtent(stack);
            swapchain = new VulkanSwapchain(physicalDevice, surface, logicalDevice, extent);
            imageViews = swapchain.stream()
                .mapToObj((image) -> new VulkanImageView(logicalDevice, image, swapchain.imageFormat()))
                .toList();
            swapchainFramebuffers = imageViews.stream()
                .map((view) -> new VulkanFramebuffer(logicalDevice, swapchain, renderPass, view, extent))
                .toList();
        }
    }

    @Override
    public void close() {
        logicalDevice.waitForIdle();

        destroySwapChain();

        frameState.forEach(FrameState::close);
        commandPool.close();
        //graphicsPipeline.close();
        renderPass.close();
        logicalDevice.close();
        physicalDevice.close();
        surface.close();
        if(debugMessenger != null) {
            debugMessenger.close();
        }
        instance.close();
    }

    @Override
    public void draw() {
        var frameState = this.frameState.get(currentFrame);
        currentFrame = (currentFrame + 1) % MAX_FRAMES_IN_FLIGHT;

        var commandBuffer = frameState.commandBuffer();
        var imageAvailableSemaphore = frameState.imageAvailableSemaphore();
        var renderFinishedSemaphore = frameState.renderFinishedSemaphore();
        var inFlightFence = frameState.inFlightFence();

        int imageIndex = swapchain.acquireNextImage(imageAvailableSemaphore);
        if(imageIndex == -1) {
            recreateSwapChain();
            imageIndex = swapchain.acquireNextImage(imageAvailableSemaphore);
        }

        inFlightFence.yield();

        commandBuffer.reset();
        commandBuffer.begin();
        commandBuffer.beginRenderPass(swapchain.extent(), renderPass, swapchainFramebuffers.get(imageIndex));
        //commandBuffer.bindPipeline(graphicsPipeline);
        commandBuffer.setViewport(swapchain.extent().width(), swapchain.extent().height());
        commandBuffer.setScissor(0, 0, swapchain.extent().width(), swapchain.extent().height());
        commandBuffer.draw(3, 1, 0, 0);
        commandBuffer.end();
        commandBuffer.submit(logicalDevice.graphicsQueue(), imageAvailableSemaphore, renderFinishedSemaphore, inFlightFence);

        try(var stack = MemoryStack.stackPush()) {
            var presentInfo = VkPresentInfoKHR.calloc(stack);
            presentInfo.sType$Default();
            presentInfo.pWaitSemaphores(stack.longs(renderFinishedSemaphore.handle()));
            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(swapchain.handle()));
            presentInfo.pImageIndices(stack.ints(imageIndex));
            var result = vkQueuePresentKHR(logicalDevice.presentQueue(), presentInfo);
            switch (result) {
                case VK_SUCCESS -> {}
                case VK_ERROR_OUT_OF_DATE_KHR, VK_SUBOPTIMAL_KHR -> recreateSwapChain();
                default -> throw new RuntimeException("Failed to present Vulkan queue: " + VulkanUtils.errorString(result));
            }
            if(framebufferResized) {
                framebufferResized = false;
                recreateSwapChain();
            }
        }
    }

    @Override
    public Optional<Shader> loadShader(@NotNull Identifier shader) {
        Map<ShaderType, ShaderMeta> metadata;
        {
            var result = ShaderMeta.load(shader);
            var partial = result.partial();
            if (partial.isPresent()) {
                LOGGER.error("Failed to load shader metadata for " + shader + '\n' + partial.get().error());
                return Optional.empty();
            }
            metadata = result.getResult();
        }

        var modules = metadata.entrySet().stream().map((entry) -> {
                var key = entry.getKey();
                var value = entry.getValue();
                // public VulkanShaderModule(@NotNull VulkanLogicalDevice device, @NotNull Type type, @NotNull Identifier identifier) {
                var type = switch (key) {
                    case FRAGMENT -> VulkanShaderModule.Type.FRAGMENT;
                    case VERTEX -> VulkanShaderModule.Type.VERTEX;
                };
                return new VulkanShaderModule(
                    logicalDevice,
                    type,
                    value.files().get("vulkan").normalize("shader", '.' + key.extension())
                );
            })
            .collect(Collectors.toUnmodifiableMap(VulkanShaderModule::type, Function.identity()));

        try(var stack = MemoryStack.stackPush()) {
            return Optional.of(new VkShader(new VulkanGraphicsPipeline(
                logicalDevice,
                physicalDevice.surfaceExtent(stack),
                renderPass,
                modules
            )));
        }
    }
}

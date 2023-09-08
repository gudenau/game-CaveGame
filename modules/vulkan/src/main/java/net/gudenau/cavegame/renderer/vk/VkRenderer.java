package net.gudenau.cavegame.renderer.vk;

import net.gudenau.cavegame.logger.Logger;
import net.gudenau.cavegame.renderer.*;
import net.gudenau.cavegame.renderer.shader.Shader;
import net.gudenau.cavegame.renderer.shader.ShaderMeta;
import net.gudenau.cavegame.resource.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPresentInfoKHR;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

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
    @Nullable
    private FrameState currentFrameState;
    private int currentImageIndex;

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
            logicalDevice = new VulkanLogicalDevice(physicalDevice);
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
    public void begin() {
        currentFrameState = this.frameState.get(currentFrame);
        currentFrame = (currentFrame + 1) % MAX_FRAMES_IN_FLIGHT;

        var commandBuffer = currentFrameState.commandBuffer();
        var imageAvailableSemaphore = currentFrameState.imageAvailableSemaphore();
        var inFlightFence = currentFrameState.inFlightFence();

        int imageIndex = swapchain.acquireNextImage(imageAvailableSemaphore);
        //TODO Fix this
        if(imageIndex == -1) {
            recreateSwapChain();
        }
        currentImageIndex = imageIndex;

        inFlightFence.yield();

        commandBuffer.reset();
        commandBuffer.begin();
    }

    @Override
    public void waitForIdle() {
        logicalDevice.waitForIdle();
    }

    @Override
    public void drawBuffer(int vertexCount, @NotNull GraphicsBuffer vertexBuffer, @Nullable GraphicsBuffer indexBuffer) {
        var commandBuffer = currentFrameState.commandBuffer;
        commandBuffer.beginRenderPass(swapchain.extent(), renderPass, swapchainFramebuffers.get(currentImageIndex));
        commandBuffer.bindPipeline(((VkShader) vertexBuffer.shader()).pipeline());
        commandBuffer.setViewport(swapchain.extent().width(), swapchain.extent().height());
        commandBuffer.setScissor(0, 0, swapchain.extent().width(), swapchain.extent().height());
        commandBuffer.bindVertexBuffer((VkGraphicsBuffer) vertexBuffer);
        if(indexBuffer != null) {
            commandBuffer.bindIndexBuffer((VkGraphicsBuffer) indexBuffer);
            commandBuffer.drawIndexed(vertexCount, 1, 0, 0, 0);
        } else {
            commandBuffer.draw(vertexCount, 1, 0, 0);
        }
    }

    @Override
    public void draw() {
        var commandBuffer = currentFrameState.commandBuffer();
        var renderFinishedSemaphore = currentFrameState.renderFinishedSemaphore();
        var imageAvailableSemaphore = currentFrameState.imageAvailableSemaphore();
        var inFlightFence = currentFrameState.inFlightFence();

        commandBuffer.endRenderPass();
        commandBuffer.submit(logicalDevice.graphicsQueue(), imageAvailableSemaphore, renderFinishedSemaphore, inFlightFence);

        try(var stack = MemoryStack.stackPush()) {
            var presentInfo = VkPresentInfoKHR.calloc(stack);
            presentInfo.sType$Default();
            presentInfo.pWaitSemaphores(stack.longs(renderFinishedSemaphore.handle()));
            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(swapchain.handle()));
            presentInfo.pImageIndices(stack.ints(currentImageIndex));
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

        currentFrameState = null;
        currentImageIndex = -1;
    }

    @Override
    public Shader loadShader(@NotNull Identifier shader) {
        ShaderMeta metadata;
        {
            var result = ShaderMeta.load(shader);
            var partial = result.partial();
            if (partial.isPresent()) {
                throw new RuntimeException("Failed to load shader metadata for " + shader + '\n' + partial.get().error());
            }
            metadata = result.getResult();
        }

        var modules = new HashMap<VulkanShaderModule.Type, VulkanShaderModule>();
        var required = new HashSet<>(VulkanShaderModule.Type.REQUIRED);
        metadata.shaders().forEach((type, info) -> {
            var vkType = switch(type) {
                case VERTEX -> VulkanShaderModule.Type.VERTEX;
                case FRAGMENT -> VulkanShaderModule.Type.FRAGMENT;
            };
            required.remove(vkType);
            modules.put(vkType, new VulkanShaderModule(
                logicalDevice,
                vkType,
                info.files().get("vulkan").normalize("shader", '.' + type.extension())
            ));
        });
        if(!required.isEmpty()) {
            throw new RuntimeException(
                "Failed to load shader, required shaders where missing: " +
                    required.stream()
                        .map((missing) -> missing.name().toLowerCase())
                        .collect(Collectors.joining(", "))
            );
        }

        var vertex = modules.get(VulkanShaderModule.Type.VERTEX);
        var vertexFormat = new VkVertexFormat(vertex, metadata.attributes());

        try(var stack = MemoryStack.stackPush()) {
            var pipeline = new VulkanGraphicsPipeline(
                logicalDevice,
                physicalDevice.surfaceExtent(stack),
                renderPass,
                modules.values(),
                vertexFormat
            );

            return new VkShader(this, pipeline, vertexFormat);
        } finally {
            modules.values().forEach(VulkanShaderModule::close);
        }
    }

    @NotNull
    @Override
    public VkGraphicsBuffer createBuffer(@NotNull BufferType type, int size) {
        return new VkGraphicsBuffer(logicalDevice, commandPool, type, size);
    }
}

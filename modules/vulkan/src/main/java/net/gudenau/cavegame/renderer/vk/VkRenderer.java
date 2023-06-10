package net.gudenau.cavegame.renderer.vk;

import net.gudenau.cavegame.renderer.Renderer;
import net.gudenau.cavegame.resource.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPresentInfoKHR;

import java.util.List;
import java.util.stream.IntStream;

import static net.gudenau.cavegame.resource.Identifier.CAVEGAME_NAMESPACE;
import static org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR;
import static org.lwjgl.vulkan.VK10.*;

public final class VkRenderer implements Renderer {
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
    private final VulkanSwapchain swapchain;
    @NotNull
    private final List<@NotNull VulkanImageView> imageViews;
    @NotNull
    private final VulkanRenderPass renderPass;
    @NotNull
    private final VulkanGraphicsPipeline graphicsPipeline;
    @NotNull
    private final List<@NotNull VulkanFramebuffer> swapchainFramebuffers;
    @NotNull
    private final VulkanCommandPool commandPool;

    private static final int MAX_FRAMES_IN_FLIGHT = 2;
    private final List<FrameState> frameState;

    private int currentFrame = 0;

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
        instance = new VulkanInstance();
        debugMessenger = VulkanUtils.ENABLE_DEBUG ? new VulkanDebugMessenger(instance) : null;
        surface = new VulkanSurface(window, instance);
        physicalDevice = VulkanPhysicalDevice.pick(instance, surface, window);
        logicalDevice = new VulkanLogicalDevice(instance, physicalDevice);
        swapchain = new VulkanSwapchain(physicalDevice, surface, logicalDevice);
        imageViews = swapchain.stream()
            .mapToObj((image) -> new VulkanImageView(logicalDevice, image, swapchain.imageFormat()))
            .toList();
        renderPass = new VulkanRenderPass(logicalDevice, swapchain);

        var shaderId = new Identifier(CAVEGAME_NAMESPACE, "basic");
        try(
            var vertexShader = new VulkanShaderModule(logicalDevice, VulkanShaderModule.Type.VERTEX, shaderId);
            var fragmentShader = new VulkanShaderModule(logicalDevice, VulkanShaderModule.Type.FRAGMENT, shaderId);
        ) {
            graphicsPipeline = new VulkanGraphicsPipeline(logicalDevice, physicalDevice.surfaceExtent(), renderPass, vertexShader, fragmentShader);
        }
        swapchainFramebuffers = imageViews.stream()
            .map((view) -> new VulkanFramebuffer(logicalDevice, swapchain, renderPass, view))
            .toList();
        commandPool = new VulkanCommandPool(physicalDevice, logicalDevice);

        frameState = IntStream.range(0, MAX_FRAMES_IN_FLIGHT)
            .mapToObj((index) -> new FrameState(index, logicalDevice, commandPool))
            .toList();
    }

    @Override
    public void close() {
        logicalDevice.waitForIdle();

        frameState.forEach(FrameState::close);
        commandPool.close();
        swapchainFramebuffers.forEach(VulkanFramebuffer::close);
        graphicsPipeline.close();
        renderPass.close();
        imageViews.forEach(VulkanImageView::close);
        swapchain.close();
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

        inFlightFence.yield();
        int imageIndex = swapchain.acquireNextImage(imageAvailableSemaphore);

        commandBuffer.reset();
        commandBuffer.begin();
        commandBuffer.beginRenderPass(swapchain.extent(), renderPass, swapchainFramebuffers.get(imageIndex));
        commandBuffer.bindPipeline(graphicsPipeline);
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
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to present Vulkan queue: " + VulkanUtils.errorString(result));
            }
        }
    }
}

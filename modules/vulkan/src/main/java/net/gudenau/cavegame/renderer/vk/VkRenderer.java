package net.gudenau.cavegame.renderer.vk;

import net.gudenau.cavegame.renderer.Renderer;
import net.gudenau.cavegame.resource.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPresentInfoKHR;

import java.util.List;

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
    @NotNull
    private final VulkanCommandBuffer commandBuffer;
    @NotNull
    private final VulkanSemaphore imageAvailableSemaphore;
    @NotNull
    private final VulkanSemaphore renderFinishedSemaphore;
    @NotNull
    private final VulkanFence inFlightFence;

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

        commandBuffer = new VulkanCommandBuffer(logicalDevice, commandPool);

        imageAvailableSemaphore = new VulkanSemaphore(logicalDevice);
        renderFinishedSemaphore = new VulkanSemaphore(logicalDevice);
        inFlightFence = new VulkanFence(logicalDevice);
    }

    @Override
    public void close() {
        logicalDevice.waitForIdle();

        inFlightFence.close();
        renderFinishedSemaphore.close();
        imageAvailableSemaphore.close();
        commandBuffer.close();
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

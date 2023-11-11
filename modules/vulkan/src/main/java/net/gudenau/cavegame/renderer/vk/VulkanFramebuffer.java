package net.gudenau.cavegame.renderer.vk;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public final class VulkanFramebuffer implements AutoCloseable {
    private final VulkanLogicalDevice device;
    private final long handle;

    public VulkanFramebuffer(
        @NotNull VulkanLogicalDevice device,
        @NotNull VulkanSwapchain swapchain,
        @NotNull VulkanRenderPass renderPass,
        @NotNull VulkanImageView imageView,
        @NotNull VulkanImageBuffer depthBuffer,
        @NotNull VulkanImageBuffer colorBuffer
    ) {
        this.device = device;
        var extent = swapchain.extent();

        try(var stack = MemoryStack.stackPush()) {
            var framebufferInfo = VkFramebufferCreateInfo.calloc(stack);
            framebufferInfo.sType$Default();
            framebufferInfo.renderPass(renderPass.handle());
            framebufferInfo.pAttachments(stack.longs(
                colorBuffer.view().handle(),
                depthBuffer.view().handle(),
                imageView.handle()
            ));
            framebufferInfo.width(extent.width());
            framebufferInfo.height(extent.height());
            framebufferInfo.layers(1);

            var pointer = stack.longs(0);
            var result = vkCreateFramebuffer(device.handle(), framebufferInfo, VulkanAllocator.get(), pointer);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create Vulkan framebuffer: " + VulkanUtils.errorString(result));
            }
            handle = pointer.get(0);
        }
    }

    public long handle() {
        return handle;
    }

    @Override
    public void close() {
        vkDestroyFramebuffer(device.handle(), handle, VulkanAllocator.get());
    }
}

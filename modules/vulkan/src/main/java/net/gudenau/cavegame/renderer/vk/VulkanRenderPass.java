package net.gudenau.cavegame.renderer.vk;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public final class VulkanRenderPass implements AutoCloseable {
    @NotNull
    private final VulkanLogicalDevice device;
    private final long handle;

    public VulkanRenderPass(@NotNull VulkanLogicalDevice device, @NotNull VulkanSwapchain swapchain) {
        this.device = device;

        try(var stack = MemoryStack.stackPush()) {
            var colorAttachment = VkAttachmentDescription.calloc(1, stack);
            colorAttachment.format(swapchain.imageFormat());
            colorAttachment.samples(VK_SAMPLE_COUNT_1_BIT);
            colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            colorAttachment.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            var colorAttachmentRef = VkAttachmentReference.calloc(1, stack);
            colorAttachmentRef.attachment(0);
            colorAttachmentRef.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            var subpass = VkSubpassDescription.calloc(1, stack);
            subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
            subpass.colorAttachmentCount(1);
            subpass.pColorAttachments(colorAttachmentRef);

            var dependency = VkSubpassDependency.calloc(1, stack);
            dependency.srcSubpass(VK_SUBPASS_EXTERNAL);
            dependency.dstSubpass(0);
            dependency.srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            dependency.srcAccessMask(0);
            dependency.dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            dependency.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

            var renderPassInfo = VkRenderPassCreateInfo.calloc(stack);
            renderPassInfo.sType$Default();
            renderPassInfo.pAttachments(colorAttachment);
            renderPassInfo.pSubpasses(subpass);
            renderPassInfo.pDependencies(dependency);

            var pointer = stack.longs(0);
            var result = vkCreateRenderPass(device.handle(), renderPassInfo, VulkanAllocator.get(), pointer);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create Vulkan render pass: " + VulkanUtils.errorString(result));
            }
            handle = pointer.get(0);
        }
    }

    public long handle() {
        return handle;
    }

    @Override
    public void close() {
        vkDestroyRenderPass(device.handle(), handle, VulkanAllocator.get());
    }
}

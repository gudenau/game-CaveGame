package net.gudenau.cavegame.renderer.vk;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK10.*;

public final class VulkanCommandBuffer implements AutoCloseable {
    @NotNull
    private final VulkanLogicalDevice device;
    @NotNull
    private final VulkanCommandPool commandPool;
    @NotNull
    private final VkCommandBuffer handle;

    public VulkanCommandBuffer(@NotNull VulkanLogicalDevice device, @NotNull VulkanCommandPool commandPool) {
        this.device = device;
        this.commandPool = commandPool;

        try(var stack = MemoryStack.stackPush()) {
            var allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
            allocInfo.sType$Default();
            allocInfo.commandPool(commandPool.handle());
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(1);

            var pointer = stack.pointers(0);
            var result = vkAllocateCommandBuffers(device.handle(), allocInfo, pointer);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create Vulkan command buffer: " + VulkanUtils.errorString(result));
            }
            handle = new VkCommandBuffer(pointer.get(0), device.handle());
        }
    }

    public void reset() {
        var result = vkResetCommandBuffer(handle, 0);
        if(result != VK_SUCCESS) {
            throw new RuntimeException("Failed to reset Vulkan command buffer: " + VulkanUtils.errorString(result));
        }
    }

    public void begin() {
        try(var stack = MemoryStack.stackPush()) {
            var beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType$Default();

            var result = vkBeginCommandBuffer(handle, beginInfo);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to begin Vulkan command buffer: " + VulkanUtils.errorString(result));
            }
        }
    }

    public void end() {
        vkCmdEndRenderPass(handle);
        var result = vkEndCommandBuffer(handle);
        if(result != VK_SUCCESS) {
            throw new RuntimeException("Failed to end Vulkan command buffer");
        }
    }

    public void beginRenderPass(@NotNull VkExtent2D extent, @NotNull VulkanRenderPass renderPass, @NotNull VulkanFramebuffer framebuffer) {
        try(var stack = MemoryStack.stackPush()) {
            var beginInfo = VkRenderPassBeginInfo.calloc(stack);
            beginInfo.sType$Default();
            beginInfo.renderPass(renderPass.handle());
            beginInfo.framebuffer(framebuffer.handle());
            beginInfo.renderArea().offset().set(0, 0);
            beginInfo.renderArea().extent().set(extent);

            var clearColor = VkClearValue.calloc(1, stack);
            clearColor.color().float32().put(new float[]{0, 0, 0, 1});
            beginInfo.pClearValues(clearColor);

            vkCmdBeginRenderPass(handle, beginInfo, VK_SUBPASS_CONTENTS_INLINE);
        }
    }

    public void bindPipeline(@NotNull VulkanGraphicsPipeline pipeline) {
        vkCmdBindPipeline(handle, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle());
    }

    public void setViewport(int width, int height) {
        try(var stack = MemoryStack.stackPush()) {
            var viewport = VkViewport.calloc(1, stack);
            // Idea is being over-zealous here
            //noinspection resource
            viewport.get(0).set(
                0, 0,
                width, height,
                0, 1
            );
            vkCmdSetViewport(handle, 0, viewport);
        }
    }

    public void setScissor(int x, int y, int width, int height) {
        try(var stack = MemoryStack.stackPush()) {
            var viewport = VkRect2D.calloc(1, stack);
            viewport.offset().set(x, y);
            viewport.extent().set(width, height);
            vkCmdSetScissor(handle, 0, viewport);
        }
    }

    public void draw(int vertexCount, int instanceCount, int firstVertex, int firstInstance) {
        vkCmdDraw(handle, vertexCount, instanceCount, firstVertex, firstInstance);
    }

    public void submit(@NotNull VkQueue queue, @NotNull VulkanSemaphore imageSemaphore, @NotNull VulkanSemaphore finishedSemaphore, @NotNull VulkanFence fence) {
        try(var stack = MemoryStack.stackPush()) {
            var submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType$Default();
            submitInfo.waitSemaphoreCount(1);
            submitInfo.pWaitSemaphores(stack.longs(imageSemaphore.handle()));
            submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));
            submitInfo.pCommandBuffers(stack.pointers(handle));
            submitInfo.pSignalSemaphores(stack.longs(finishedSemaphore.handle()));
            var result = vkQueueSubmit(queue, submitInfo, fence.handle());
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to submit Vulkan queue: " + VulkanUtils.errorString(result));
            }
        }
    }

    @NotNull
    public VkCommandBuffer handle() {
        return handle;
    }

    @Override
    public void close() {
        vkFreeCommandBuffers(device.handle(), commandPool.handle(), handle);
    }
}

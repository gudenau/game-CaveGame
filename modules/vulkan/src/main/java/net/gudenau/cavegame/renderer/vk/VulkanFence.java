package net.gudenau.cavegame.renderer.vk;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public final class VulkanFence implements AutoCloseable {
    @NotNull
    private final VulkanLogicalDevice device;
    private final long handle;

    public VulkanFence(@NotNull VulkanLogicalDevice device) {
        this.device = device;

        try(var stack = MemoryStack.stackPush()) {
            var createInfo = VkFenceCreateInfo.calloc(stack);
            createInfo.sType$Default();
            createInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

            var pointer = stack.longs(0);
            var result = vkCreateFence(device.handle(), createInfo, VulkanAllocator.get(), pointer);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create Vulkan fence: " + VulkanUtils.errorString(result));
            }
            handle = pointer.get(0);
        }
    }

    public void yield() {
        try(var stack = MemoryStack.stackPush()) {
            var fences = stack.longs(handle);
            // timeout is unsigned, -1 is equal to the max u64.
            vkWaitForFences(device.handle(), fences, true, -1L);
            vkResetFences(device.handle(), fences);
        }
    }

    public long handle() {
        return handle;
    }

    @Override
    public void close() {
        vkDestroyFence(device.handle(), handle, VulkanAllocator.get());
    }
}

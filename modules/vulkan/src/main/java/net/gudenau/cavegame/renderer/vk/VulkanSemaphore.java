package net.gudenau.cavegame.renderer.vk;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public final class VulkanSemaphore implements AutoCloseable {
    @NotNull
    private final VulkanLogicalDevice device;
    private final long handle;

    public VulkanSemaphore(@NotNull VulkanLogicalDevice device) {
        this.device = device;

        try(var stack = MemoryStack.stackPush()) {
            var createInfo = VkSemaphoreCreateInfo.calloc(stack);
            createInfo.sType$Default();

            var pointer = stack.longs(0);
            var result = vkCreateSemaphore(device.handle(), createInfo, VulkanAllocator.get(), pointer);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create Vulkan semaphore: " + VulkanUtils.errorString(result));
            }
            handle = pointer.get(0);
        }
    }

    public long handle() {
        return handle;
    }

    @Override
    public void close() {
        vkDestroySemaphore(device.handle(), handle, VulkanAllocator.get());
    }
}

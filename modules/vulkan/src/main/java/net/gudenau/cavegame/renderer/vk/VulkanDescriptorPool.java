package net.gudenau.cavegame.renderer.vk;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;

import static org.lwjgl.vulkan.VK10.*;

public final class VulkanDescriptorPool implements AutoCloseable {
    private final VulkanLogicalDevice device;
    private final long handle;

    public VulkanDescriptorPool(@NotNull VulkanLogicalDevice device, int type, int size) {
        this.device = device;

        try(var stack = MemoryStack.stackPush()) {
            var poolSizes = VkDescriptorPoolSize.calloc(1, stack);
            //noinspection resource
            poolSizes.get(0).set(type, size);

            var poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
            poolInfo.sType$Default();
            poolInfo.pPoolSizes(poolSizes);
            poolInfo.maxSets(size);
            poolInfo.flags(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT);

            var pointer = stack.longs(0);
            var result = vkCreateDescriptorPool(device.handle(), poolInfo, VulkanAllocator.get(), pointer);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor pool: " + VulkanUtils.errorString(result));
            }
            handle = pointer.get(0);
        }
    }

    @Override
    public void close() {
        vkDestroyDescriptorPool(device.handle(), handle, VulkanAllocator.get());
    }

    public long handle() {
        return handle;
    }
}

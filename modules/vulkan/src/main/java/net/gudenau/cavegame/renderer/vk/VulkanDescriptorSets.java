package net.gudenau.cavegame.renderer.vk;

import net.gudenau.cavegame.util.BufferUtil;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public final class VulkanDescriptorSets implements AutoCloseable {
    private final VulkanLogicalDevice device;
    private final VulkanDescriptorPool pool;
    private final long[] handles;

    public VulkanDescriptorSets(@NotNull VulkanLogicalDevice device, @NotNull VulkanDescriptorPool pool, @NotNull LongBuffer layouts) {
        this.device = device;
        this.pool = pool;

        try(var stack = MemoryStack.stackPush()) {
            var allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
            allocInfo.sType$Default();
            allocInfo.descriptorPool(pool.handle());
            allocInfo.pSetLayouts(layouts);

            var handlesPointer = stack.callocLong(layouts.remaining());
            var result = vkAllocateDescriptorSets(device.handle(), allocInfo, handlesPointer);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate descriptor sets: " + VulkanUtils.errorString(result));
            }
            handles = BufferUtil.toArray(handlesPointer);
        }
    }

    public long get(int index) {
        return handles[index];
    }

    @Override
    public void close() {
        try(var stack = MemoryStack.stackPush()) {
            vkFreeDescriptorSets(device.handle(), pool.handle(), stack.longs(handles));
        }
    }
}

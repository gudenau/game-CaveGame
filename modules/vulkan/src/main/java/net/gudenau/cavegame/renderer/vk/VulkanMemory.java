package net.gudenau.cavegame.renderer.vk;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanMemory implements AutoCloseable {
    private final VulkanLogicalDevice device;

    private final long size;
    private final long handle;

    public VulkanMemory(VulkanLogicalDevice device, VkMemoryRequirements requirements, int properties) {
        this.size = requirements.size();
        this.device = device;

        try(var stack = MemoryStack.stackPush()) {
            var allocInfo = VkMemoryAllocateInfo.calloc(stack);
            allocInfo.sType$Default();
            allocInfo.allocationSize(size);
            allocInfo.memoryTypeIndex(determineMemoryType(
                requirements.memoryTypeBits(),
                properties
            ));

            var memoryPointer = stack.longs(0);
            var result = vkAllocateMemory(device.handle(), allocInfo, VulkanAllocator.get(), memoryPointer);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate memory: " + VulkanUtils.errorString(result));
            }
            handle = memoryPointer.get();
        }
    }

    private int determineMemoryType(int filter, int flags) {
        try(var stack = MemoryStack.stackPush()) {
            var properties = VkPhysicalDeviceMemoryProperties.calloc(stack);
            vkGetPhysicalDeviceMemoryProperties(device.device().device(), properties);

            var types = properties.memoryTypes();
            var count = properties.memoryTypeCount();
            for(int i = 0; i < count; i++) {
                if(
                    (filter & (1 << i)) != 0 &&
                        (types.get(i).propertyFlags() & flags) == flags
                ) {
                    return i;
                }
            }
        }

        throw new RuntimeException("Failed to find memory type");
    }

    @Override
    public void close() {
        vkFreeMemory(device.handle(), handle, VulkanAllocator.get());
    }

    public long handle() {
        return handle;
    }

    public ByteBuffer map() {
        if(size > Integer.MAX_VALUE) {
            throw new IllegalStateException("Memory size is too large for NIO buffers");
        }

        try(var stack = MemoryStack.stackPush()) {
            var bufferPointer = stack.pointers(0);
            var result = vkMapMemory(device.handle(), handle, 0, size, 0, bufferPointer);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to map Vulkan memory: " + VulkanUtils.errorString(result));
            }
            return bufferPointer.getByteBuffer(0, (int) size);
        }
    }

    public void unmap() {
        vkUnmapMemory(device.handle(), handle);
    }

    public void upload(ByteBuffer data) {
        if(data.remaining() > size) {
            throw new IllegalArgumentException("Provided buffer was too large for allocate memory");
        }

        var buffer = map();
        try {
            MemoryUtil.memCopy(data, buffer);
        } finally {
            unmap();
        }
    }

    public long size() {
        return size;
    }
}

package net.gudenau.cavegame.renderer.vk;

import net.gudenau.cavegame.renderer.BufferType;
import net.gudenau.cavegame.renderer.GraphicsBuffer;
import net.gudenau.cavegame.renderer.shader.Shader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VkGraphicsBuffer implements GraphicsBuffer {
    private final VulkanLogicalDevice device;
    private final int size;

    private final long handle;
    private final long memory;

    private VkShader shader;

    VkGraphicsBuffer(@NotNull VulkanLogicalDevice device, @NotNull BufferType type, int size) {
        this.device = device;
        this.size = size;

        try(var stack = MemoryStack.stackPush()) {
            var bufferInfo = VkBufferCreateInfo.calloc(stack);
            bufferInfo.sType$Default();
            bufferInfo.size(size);
            bufferInfo.usage(switch (type) {
                case VERTEX -> VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
            });
            bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            var handlePointer = stack.longs(0);
            var result = vkCreateBuffer(device.handle(), bufferInfo, VulkanAllocator.get(), handlePointer);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create buffer: " + VulkanUtils.errorString(result));
            }
            handle = handlePointer.get(0);

            var memoryRequirements = VkMemoryRequirements.calloc(stack);
            vkGetBufferMemoryRequirements(device.handle(), handle, memoryRequirements);

            var allocationInfo = VkMemoryAllocateInfo.calloc(stack);
            allocationInfo.sType$Default();
            allocationInfo.allocationSize(memoryRequirements.size());
            allocationInfo.memoryTypeIndex(determineMemoryType(
                memoryRequirements.memoryTypeBits(),
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
            ));

            var memoryPointer = stack.longs(0);
            result = vkAllocateMemory(device.handle(), allocationInfo, VulkanAllocator.get(), memoryPointer);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate buffer: " + VulkanUtils.errorString(result));
            }
            memory = memoryPointer.get(0);

            result = vkBindBufferMemory(device.handle(), handle, memory, 0);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to bind buffer: " + VulkanUtils.errorString(result));
            }
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

    public void shader(@NotNull VkShader shader) {
        this.shader = shader;
    }

    @Override
    public void upload(@NotNull ByteBuffer data) {
        try(var stack = MemoryStack.stackPush()) {
            var size = Math.min(data.remaining(), this.size);
            var bufferPointer = stack.pointers(0);
            var result = vkMapMemory(device.handle(), memory, 0, size, 0, bufferPointer);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to map Vulkan memory: " + VulkanUtils.errorString(result));
            }

            try {
                var buffer = bufferPointer.getByteBuffer(size);
                MemoryUtil.memCopy(data, buffer);
            } finally {
                vkUnmapMemory(device.handle(), memory);
            }
        }
    }

    @Override
    public @Nullable Shader shader() {
        return shader;
    }

    @Override
    public void close() {
        vkDestroyBuffer(device.handle(), handle, VulkanAllocator.get());
        vkFreeMemory(device.handle(), memory, VulkanAllocator.get());
    }

    public long handle() {
        return handle;
    }
}

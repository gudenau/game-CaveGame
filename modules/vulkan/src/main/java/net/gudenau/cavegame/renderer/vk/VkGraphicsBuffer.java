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
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VkGraphicsBuffer implements GraphicsBuffer {
    private final VulkanLogicalDevice device;
    private final VulkanCommandPool commandPool;
    private final int size;

    private final long handle;
    private final long memory;

    private VkShader shader;

    VkGraphicsBuffer(@NotNull VulkanLogicalDevice device, @NotNull VulkanCommandPool commandPool, @NotNull BufferType type, int size) {
        this.device = device;
        this.commandPool = commandPool;
        this.size = size;

        try(var stack = MemoryStack.stackPush()) {
            var handlePointer = stack.longs(0);
            var memoryPointer = stack.longs(0);
            createBuffer(
                switch(type) {
                    case VERTEX -> VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
                } | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
                VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                handlePointer,
                memoryPointer
            );
            handle = handlePointer.get(0);
            memory = memoryPointer.get(0);
        }
    }

    private void createBuffer(int usage, int properties, @NotNull LongBuffer handle, @NotNull LongBuffer memory) {
        try(var stack = MemoryStack.stackPush()) {
            var bufferInfo = VkBufferCreateInfo.calloc(stack);
            bufferInfo.sType$Default();
            bufferInfo.size(size);
            bufferInfo.usage(usage);
            bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            var result = vkCreateBuffer(device.handle(), bufferInfo, VulkanAllocator.get(), handle);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create buffer: " + VulkanUtils.errorString(result));
            }
            var buffer = handle.get(0);

            var memoryRequirements = VkMemoryRequirements.calloc(stack);
            vkGetBufferMemoryRequirements(device.handle(), buffer, memoryRequirements);

            var allocationInfo = VkMemoryAllocateInfo.calloc(stack);
            allocationInfo.sType$Default();
            allocationInfo.allocationSize(memoryRequirements.size());
            allocationInfo.memoryTypeIndex(determineMemoryType(
                memoryRequirements.memoryTypeBits(),
                properties
            ));

            result = vkAllocateMemory(device.handle(), allocationInfo, VulkanAllocator.get(), memory);
            if(result != VK_SUCCESS) {
                vkDestroyBuffer(device.handle(), handle.get(0), VulkanAllocator.get());
                throw new RuntimeException("Failed to allocate buffer: " + VulkanUtils.errorString(result));
            }

            result = vkBindBufferMemory(device.handle(), buffer, memory.get(0), 0);
            if(result != VK_SUCCESS) {
                vkDestroyBuffer(device.handle(), handle.get(0), VulkanAllocator.get());
                vkFreeMemory(device.handle(), memory.get(0), VulkanAllocator.get());
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
            var stagingPointer = stack.longs(0);
            var stagingMemoryPointer = stack.longs(0);
            createBuffer(
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                stagingPointer,
                stagingMemoryPointer
            );
            var staging = stagingPointer.get(0);
            var stagingMemory = stagingMemoryPointer.get(0);

            var size = Math.min(data.remaining(), this.size);
            var bufferPointer = stack.pointers(0);
            var result = vkMapMemory(device.handle(), stagingMemory, 0, size, 0, bufferPointer);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to map Vulkan memory: " + VulkanUtils.errorString(result));
            }

            try {
                var buffer = bufferPointer.getByteBuffer(size);
                MemoryUtil.memCopy(data, buffer);
            } finally {
                vkUnmapMemory(device.handle(), stagingMemory);
            }

            try(var commandBuffer = new VulkanCommandBuffer(device, commandPool)) {
                commandBuffer.begin();
                commandBuffer.copyBuffer(staging, 0, handle, 0, size);
                commandBuffer.end();
                commandBuffer.submit(device.graphicsQueue());
            }

            vkDestroyBuffer(device.handle(), staging, VulkanAllocator.get());
            vkFreeMemory(device.handle(), stagingMemory, VulkanAllocator.get());
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

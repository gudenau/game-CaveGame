package net.gudenau.cavegame.renderer.vk;

import net.gudenau.cavegame.renderer.BufferType;
import net.gudenau.cavegame.renderer.GraphicsBuffer;
import net.gudenau.cavegame.renderer.shader.Shader;
import net.gudenau.cavegame.util.Treachery;
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
    private final boolean empherial;
    private final int size;

    private final long handle;
    private final VulkanMemory memory;

    private VkShader shader;

    VkGraphicsBuffer(@NotNull VulkanLogicalDevice device, @NotNull VulkanCommandPool commandPool, @NotNull BufferType type, int size) {
        this.device = device;
        this.commandPool = commandPool;
        this.empherial = switch(type) {
            case UNIFORM, STAGING -> true;
            default -> false;
        };
        this.size = size;

        try(var stack = MemoryStack.stackPush()) {
            var handlePointer = stack.longs(0);
            memory = createBuffer(
                switch(type) {
                    case VERTEX -> VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT;
                    case INDEX -> VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT;
                    case UNIFORM -> VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT;
                    case STAGING -> VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
                },
                empherial ? VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT :
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                handlePointer
            );
            handle = handlePointer.get(0);
        }
    }

    private VulkanMemory createBuffer(int usage, int properties, @NotNull LongBuffer handle) {
        VulkanMemory memory;

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

            memory = new VulkanMemory(device, memoryRequirements, properties);

            result = vkBindBufferMemory(device.handle(), buffer, memory.handle(), 0);
            if(result != VK_SUCCESS) {
                vkDestroyBuffer(device.handle(), handle.get(0), VulkanAllocator.get());
                memory.close();
                throw new RuntimeException("Failed to bind buffer: " + VulkanUtils.errorString(result));
            }
        }

        return memory;
    }

    public void shader(@NotNull VkShader shader) {
        this.shader = shader;
    }

    @Override
    public void upload(@NotNull ByteBuffer data) {
        if(empherial) {
            memory.upload(data);
        } else {
            doUpload(data);
        }
    }

    public int size() {
        return size;
    }

    private void doUpload(ByteBuffer data) {
        try(var stack = MemoryStack.stackPush()) {
            var stagingPointer = stack.longs(0);
            var stagingMemory = createBuffer(
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                stagingPointer
            );
            var staging = stagingPointer.get(0);

            stagingMemory.upload(data);

            try(var commandBuffer = new VulkanCommandBuffer(device, commandPool)) {
                commandBuffer.begin();
                commandBuffer.copyBuffer(staging, 0, handle, 0, size);
                commandBuffer.end();
                commandBuffer.submit(device.graphicsQueue());
                vkQueueWaitIdle(device.graphicsQueue());
            }

            vkDestroyBuffer(device.handle(), staging, VulkanAllocator.get());
            stagingMemory.close();
        }
    }

    @Override
    public @Nullable Shader shader() {
        return shader;
    }

    @Override
    public void close() {
        vkDestroyBuffer(device.handle(), handle, VulkanAllocator.get());
        memory.close();
    }

    public long handle() {
        return handle;
    }
}

package net.gudenau.cavegame.renderer.vk;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkImageCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public final class VulkanImage implements AutoCloseable {
    private final VulkanLogicalDevice device;
    private final int width;
    private final int height;
    private final int format;
    private final long handle;
    @Nullable
    private final VulkanMemory memory;

    public VulkanImage(VulkanLogicalDevice device, int width, int height, int format, long handle) {
        this.device = device;
        this.format = format;
        this.width = width;
        this.height = height;
        this.handle = handle;
        memory = null;
    }

    public VulkanImage(VulkanLogicalDevice device, int width, int height, int format, int usage) {
        this.device = device;
        this.format = format;
        this.width = width;
        this.height = height;

        try(var stack = MemoryStack.stackPush()) {
            var imageInfo = VkImageCreateInfo.calloc(stack);
            imageInfo.sType$Default();
            imageInfo.imageType(VK_IMAGE_TYPE_2D);
            imageInfo.extent().set(width, height, 1);
            imageInfo.mipLevels(1);
            imageInfo.arrayLayers(1);
            imageInfo.format(format);
            imageInfo.tiling(VK_IMAGE_TILING_OPTIMAL);
            imageInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            imageInfo.usage(usage);
            imageInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            imageInfo.samples(VK_SAMPLE_COUNT_1_BIT);
            imageInfo.flags(0);

            var texturePointer = stack.longs(0);
            var result = vkCreateImage(device.handle(), imageInfo, VulkanAllocator.get(), texturePointer);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create Vulkan image: " + VulkanUtils.errorString(result));
            }
            handle = texturePointer.get(0);

            memory = VulkanMemory.ofImage(device, this, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
            vkBindImageMemory(device.handle(), handle, memory.handle(), 0);
        }
    }

    public long handle() {
        return handle;
    }

    public int format() {
        return format;
    }

    public long size() {
        return memory != null ? memory.size() : 0;
    }

    @Override
    public void close() {
        if(memory != null) {
            vkDestroyImage(device.handle(), handle, VulkanAllocator.get());
            memory.close();
        }
    }
}

package net.gudenau.cavegame.renderer.vk;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageFormatProperties;

import static org.lwjgl.vulkan.VK10.*;

public final class VulkanImage implements AutoCloseable {
    private final VulkanLogicalDevice device;
    private final int width;
    private final int height;
    private final int format;
    private final long handle;
    private final int mipLevels;
    @Nullable
    private final VulkanMemory memory;

    public VulkanImage(VulkanLogicalDevice device, int width, int height, int format, long handle) {
        this.device = device;
        this.format = format;
        this.width = width;
        this.height = height;
        this.handle = handle;
        this.mipLevels = 1;
        memory = null;
    }

    public VulkanImage(VulkanLogicalDevice device, int width, int height, int format, int usage) {
        this(device, width, height, format, usage, 1, VK_SAMPLE_COUNT_1_BIT);
    }

    public VulkanImage(VulkanLogicalDevice device, int width, int height, int format, int usage, int mipLevels) {
        this(device, width, height, format, usage, mipLevels, VK_SAMPLE_COUNT_1_BIT);
    }

    public VulkanImage(VulkanLogicalDevice device, int width, int height, int format, int usage, int mipLevels, int samples) {
        this.device = device;
        this.format = format;
        this.width = width;
        this.height = height;
        this.mipLevels = mipLevels;

        try(var stack = MemoryStack.stackPush()) {
            var imageInfo = VkImageCreateInfo.calloc(stack);
            imageInfo.sType$Default();
            imageInfo.imageType(VK_IMAGE_TYPE_2D);
            imageInfo.extent().set(width, height, 1);
            imageInfo.mipLevels(mipLevels);
            imageInfo.arrayLayers(1);
            imageInfo.format(format);
            imageInfo.tiling(VK_IMAGE_TILING_OPTIMAL);
            imageInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            imageInfo.usage(usage);
            imageInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            imageInfo.samples(samples);
            imageInfo.flags(0);

            var props = VkImageFormatProperties.calloc(stack);
            var result2 = vkGetPhysicalDeviceImageFormatProperties(device.device().device(), imageInfo.format(), imageInfo.imageType(), imageInfo.tiling(), imageInfo.usage(), imageInfo.flags(), props);

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

    public int mipLevels() {
        return mipLevels;
    }
}

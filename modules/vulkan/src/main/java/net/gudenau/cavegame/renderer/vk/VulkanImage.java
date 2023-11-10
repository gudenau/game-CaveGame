package net.gudenau.cavegame.renderer.vk;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkImageCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public final class VulkanImage implements AutoCloseable {
    private final VulkanLogicalDevice device;
    private final int width;
    private final int height;
    private final int format;
    private final long handle;
    private final boolean cleanup;

    public VulkanImage(VulkanLogicalDevice device, int width, int height, int format, long handle) {
        this.device = device;
        this.format = format;
        this.width = width;
        this.height = height;
        this.handle = handle;
        cleanup = false;
    }

    public VulkanImage(VulkanLogicalDevice device, int width, int height, int format) {
        this.device = device;
        this.format = format;
        this.width = width;
        this.height = height;
        cleanup = true;

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
            imageInfo.usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT);
            imageInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            imageInfo.samples(VK_SAMPLE_COUNT_1_BIT);
            imageInfo.flags(0);

            var texturePointer = stack.longs(0);
            var result = vkCreateImage(device.handle(), imageInfo, VulkanAllocator.get(), texturePointer);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create Vulkan image: " + VulkanUtils.errorString(result));
            }
            handle = texturePointer.get(0);
        }
    }

    public long handle() {
        return handle;
    }

    public int format() {
        return format;
    }

    @Override
    public void close() {
        if(cleanup) {
            vkDestroyImage(device.handle(), handle, VulkanAllocator.get());
        }
    }
}

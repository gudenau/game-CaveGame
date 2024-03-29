package net.gudenau.cavegame.renderer.vk;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public final class VulkanImageView implements AutoCloseable {
    @NotNull
    private final VulkanLogicalDevice device;
    private final long handle;

    public VulkanImageView(@NotNull VulkanLogicalDevice device, @NotNull VulkanImage image) {
        this(device, image, VK_IMAGE_ASPECT_COLOR_BIT);
    }

    public VulkanImageView(@NotNull VulkanLogicalDevice device, @NotNull VulkanImage image, int aspectFlags) {
        this.device = device;

        try(var stack = MemoryStack.stackPush()) {
            var createInfo = VkImageViewCreateInfo.calloc(stack);
            createInfo.sType$Default();
            createInfo.image(image.handle());
            createInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
            createInfo.format(image.format());
            createInfo.components().set(
                VK_COMPONENT_SWIZZLE_IDENTITY,
                VK_COMPONENT_SWIZZLE_IDENTITY,
                VK_COMPONENT_SWIZZLE_IDENTITY,
                VK_COMPONENT_SWIZZLE_IDENTITY
            );
            createInfo.subresourceRange().set(
                aspectFlags,
                0,
                image.mipLevels(),
                0,
                1
            );

            var pointer = stack.longs(0);
            var result = vkCreateImageView(device.handle(), createInfo, VulkanAllocator.get(), pointer);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create Vulkan image view: " + VulkanUtils.errorString(result));
            }
            this.handle = pointer.get(0);
        }
    }

    public long handle() {
        return handle;
    }

    @Override
    public void close() {
        vkDestroyImageView(device.handle(), handle, VulkanAllocator.get());
    }
}

package net.gudenau.cavegame.renderer.vk;

import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkFormatProperties;

import static org.lwjgl.vulkan.VK10.*;

public final class VulkanDepthBuffer implements AutoCloseable {
    private final VulkanImage image;
    private final VulkanImageView view;

    public VulkanDepthBuffer(@NotNull VulkanLogicalDevice device, @NotNull VulkanSwapchain swapchain) {
        var format = findDepthFormat(device.device());
        var extent = swapchain.extent();
        image = new VulkanImage(device, extent.width(), extent.height(), format, VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT);
        view = new VulkanImageView(device, image, VK_IMAGE_ASPECT_DEPTH_BIT);
    }

    private int findDepthFormat(VulkanPhysicalDevice device) {
        try(var stack = MemoryStack.stackPush()) {
            var props = VkFormatProperties.calloc(stack);
            var formats = IntList.of(VK_FORMAT_D32_SFLOAT, VK_FORMAT_D32_SFLOAT_S8_UINT, VK_FORMAT_D24_UNORM_S8_UINT);
            for(var format : formats) {
                vkGetPhysicalDeviceFormatProperties(device.device(), format, props);

                if((props.optimalTilingFeatures() & VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT) != 0) {
                    return format;
                }
            }
        }

        throw new RuntimeException("Failed to find depth buffer format");
    }

    public VulkanImage image() {
        return image;
    }

    public VulkanImageView view() {
        return view;
    }

    @Override
    public void close() {
        view.close();
        image.close();
    }
}

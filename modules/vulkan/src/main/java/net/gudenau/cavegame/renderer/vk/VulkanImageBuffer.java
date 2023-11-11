package net.gudenau.cavegame.renderer.vk;

import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkFormatProperties;

import static org.lwjgl.vulkan.VK10.*;

public final class VulkanImageBuffer implements AutoCloseable {
    private final VulkanImage image;
    private final VulkanImageView view;

    public VulkanImageBuffer(@NotNull VulkanLogicalDevice device, @NotNull VulkanSwapchain swapchain, int format, int usage) {
        var extent = swapchain.extent();
        image = new VulkanImage(
            device,
            extent.width(), extent.height(),
            format,
            usage,
            1,
            device.device().maxSampleCount()
        );
        view = new VulkanImageView(
            device,
            image,
            (usage & VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT) != 0 ?
                VK_IMAGE_ASPECT_DEPTH_BIT :
                VK_IMAGE_ASPECT_COLOR_BIT
        );
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

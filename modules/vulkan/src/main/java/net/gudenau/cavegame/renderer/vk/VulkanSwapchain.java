package net.gudenau.cavegame.renderer.vk;

import it.unimi.dsi.fastutil.longs.LongList;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import java.util.stream.LongStream;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public final class VulkanSwapchain implements AutoCloseable {
    @NotNull
    private final VulkanLogicalDevice device;
    private final long handle;
    @NotNull
    private final LongList swapChainImages;
    private final int imageFormat;
    @NotNull
    private final VkExtent2D extent;

    public VulkanSwapchain(@NotNull VulkanPhysicalDevice physicalDevice, @NotNull VulkanSurface surface, @NotNull VulkanLogicalDevice logicalDevice) {
        this.device = logicalDevice;

        try(var stack = MemoryStack.stackPush()) {
            var surfaceCapabilities = physicalDevice.surfaceCapabilities();
            var surfaceFormat = physicalDevice.surfaceFormat();

            int imageCount = Math.min(
                surfaceCapabilities.minImageCount() + 1,
                surfaceCapabilities.maxImageCount()
            );

            var createInfo = VkSwapchainCreateInfoKHR.calloc(stack);
            createInfo.sType$Default();
            createInfo.surface(surface.handle());
            createInfo.minImageCount(imageCount);
            createInfo.imageFormat(surfaceFormat.format());
            createInfo.imageColorSpace(surfaceFormat.colorSpace());
            createInfo.imageExtent(physicalDevice.surfaceExtent());
            createInfo.imageArrayLayers(1);
            createInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);

            var graphicsQueue = physicalDevice.graphicsQueue();
            var presentQueue = physicalDevice.presentQueue();
            if(graphicsQueue != presentQueue) {
                createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
                createInfo.queueFamilyIndexCount(2);
                createInfo.pQueueFamilyIndices(stack.ints(graphicsQueue, presentQueue));
            } else {
                createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
                createInfo.queueFamilyIndexCount(0);
                createInfo.pQueueFamilyIndices(null);
            }

            createInfo.preTransform(surfaceCapabilities.currentTransform());
            createInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            createInfo.presentMode(physicalDevice.surfacePresentMode());
            createInfo.clipped(true);
            createInfo.oldSwapchain(VK_NULL_HANDLE);

            var pointer = stack.longs(0);
            var result = vkCreateSwapchainKHR(logicalDevice.handle(), createInfo, VulkanAllocator.get(), pointer);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create Vulkan swapchain: " + VulkanUtils.errorString(result));
            }
            handle = pointer.get(0);

            var imageCountPointer = stack.ints(0);
            vkGetSwapchainImagesKHR(logicalDevice.handle(), handle, imageCountPointer, null);

            imageCount = imageCountPointer.get(0);
            var swapChainImages = stack.callocLong(imageCount);
            vkGetSwapchainImagesKHR(logicalDevice.handle(), handle, imageCountPointer, swapChainImages);
            this.swapChainImages = VulkanUtils.extractList(swapChainImages);

            imageFormat = surfaceFormat.format();
            extent = VkExtent2D.calloc().set(physicalDevice.surfaceExtent());
        }
    }

    public int imageFormat() {
        return imageFormat;
    }

    public int acquireNextImage(@NotNull VulkanSemaphore semaphore) {
        try(var stack = MemoryStack.stackPush()) {
            var image = stack.ints(0);
            var result = vkAcquireNextImageKHR(device.handle(), handle, -1L, semaphore.handle(), VK_NULL_HANDLE, image);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to acquire next swapchain image: " + VulkanUtils.errorString(result));
            }
            return image.get(0);
        }
    }

    @NotNull
    public VkExtent2D extent() {
        return extent;
    }

    public long handle() {
        return handle;
    }

    @NotNull
    public LongStream stream() {
        return swapChainImages.longStream();
    }

    @Override
    public void close() {
        vkDestroySwapchainKHR(device.handle(), handle, VulkanAllocator.get());
        extent.free();
    }
}

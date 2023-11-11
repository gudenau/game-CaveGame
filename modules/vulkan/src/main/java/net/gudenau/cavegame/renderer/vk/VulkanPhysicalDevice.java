package net.gudenau.cavegame.renderer.vk;

import net.gudenau.cavegame.util.MathUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.*;

public final class VulkanPhysicalDevice implements AutoCloseable {
    @NotNull
    private final VkPhysicalDevice device;
    @NotNull
    private final VulkanSurface surface;
    @NotNull
    private final VkWindow window;

    @NotNull
    private final VkPhysicalDeviceProperties deviceProperties;
    @NotNull
    private final VkPhysicalDeviceFeatures deviceFeatures;

    private final int maxSampleCount;
    private final int rank;

    private int graphicsQueue = -1;
    private int presentQueue = -1;
    @Nullable
    private VkSurfaceFormatKHR surfaceFormat = null;
    private int surfacePresentMode = -1;

    private VulkanPhysicalDevice(@NotNull VkPhysicalDevice device, @NotNull VulkanSurface surface, @NotNull VkWindow window) {
        this.device = device;
        this.surface = surface;
        this.window = window;

        deviceProperties = VkPhysicalDeviceProperties.calloc();
        deviceFeatures = VkPhysicalDeviceFeatures.calloc();

        vkGetPhysicalDeviceProperties(device, deviceProperties);
        vkGetPhysicalDeviceFeatures(device, deviceFeatures);

        maxSampleCount = getMaxSampleCount();
        findQueues();
        querySwapChainSupport();
        this.rank = calculateRank();
    }

    private int getMaxSampleCount() {
        var limits = deviceProperties.limits();
        var counts = limits.framebufferColorSampleCounts() & limits.framebufferDepthSampleCounts();
        var leadingZeros = Integer.numberOfLeadingZeros(counts);
        if(leadingZeros <= 25) {
            return VK_SAMPLE_COUNT_64_BIT;
        }
        return switch(leadingZeros) {
            case 26 -> VK_SAMPLE_COUNT_32_BIT;
            case 27 -> VK_SAMPLE_COUNT_16_BIT;
            case 28 -> VK_SAMPLE_COUNT_8_BIT;
            case 29 -> VK_SAMPLE_COUNT_4_BIT;
            case 30 -> VK_SAMPLE_COUNT_2_BIT;
            default -> VK_SAMPLE_COUNT_1_BIT;
        };
    }

    public int maxSampleCount() {
        return maxSampleCount;
    }

    private void findQueues() {
        try(var stack = MemoryStack.stackPush()) {
            var countPointer = stack.ints(0);
            vkGetPhysicalDeviceQueueFamilyProperties(device, countPointer, null);

            var count = countPointer.get(0);
            var queues = VkQueueFamilyProperties.calloc(count, stack);
            vkGetPhysicalDeviceQueueFamilyProperties(device, countPointer, queues);

            var presentSupport = stack.ints(VK_FALSE);
            for(int i = 0; i < count; i++) {
                var queue = queues.get(i);
                var queueFlags = queue.queueFlags();
                if(graphicsQueue == -1 && (queueFlags & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    graphicsQueue = i;
                }

                if(presentQueue == -1) {
                    vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface.handle(), presentSupport);
                    if (presentSupport.get() == VK_TRUE) {
                        presentQueue = i;
                    }
                }
            }
        }
    }

    private void querySwapChainSupport() {
        try (var stack = MemoryStack.stackPush()) {
            var formatCountPointer = stack.ints(0);
            vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface.handle(), formatCountPointer, null);

            var formatCount = formatCountPointer.get(0);
            if (formatCount != 0) {
                var surfaceFormats = VkSurfaceFormatKHR.calloc(formatCount, stack);
                vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface.handle(), formatCountPointer, surfaceFormats);

                VkSurfaceFormatKHR bestFormat = null;
                for (var format : surfaceFormats) {
                    if (format.format() == VK_FORMAT_B8G8R8A8_SRGB && format.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                        bestFormat = format;
                        break;
                    }
                }
                if (bestFormat == null) {
                    bestFormat = surfaceFormats.get(0);
                }
                //TODO Find a better way
                surfaceFormat = VkSurfaceFormatKHR.calloc();
                MemoryUtil.memCopy(bestFormat, surfaceFormat);
            }

            var presentModeCountPointer = stack.ints(0);
            vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface.handle(), presentModeCountPointer, null);

            var presentModeCount = presentModeCountPointer.get(0);
            if (presentModeCount != 0) {
                var presentModes = stack.callocInt(presentModeCount);
                vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface.handle(), presentModeCountPointer, presentModes);
                var surfacePresentModes = VulkanUtils.extractList(presentModes);

                surfacePresentMode = VK_PRESENT_MODE_FIFO_KHR;
                for (int presentMode : surfacePresentModes) {
                    if(presentMode == VK_PRESENT_MODE_MAILBOX_KHR) {
                        surfacePresentMode = VK_PRESENT_MODE_MAILBOX_KHR;
                        break;
                    }
                }
            }
        }
    }

    private void chooseSwpExtent(@NotNull VkExtent2D extent) {
        try(var stack = MemoryStack.stackPush()) {
            var capabilities = surfaceCapabilities(stack);
            if (capabilities.currentExtent().width() != 0xFFFFFFFF) {
                extent.set(capabilities.currentExtent());
            } else {
                var size = window.framebufferSize();
                var min = capabilities.minImageExtent();
                var max = capabilities.maxImageExtent();

                extent.set(
                    MathUtils.clamp(size.width(), min.width(), max.width()),
                    MathUtils.clamp(size.height(), min.height(), max.height())
                );
            }
        }
    }

    @NotNull
    public VkPhysicalDevice device() {
        return device;
    }

    private int rank() {
        return rank;
    }

    private int calculateRank() {
        if(graphicsQueue == -1 || presentQueue == -1) {
            return -1;
        }

        if(!deviceFeatures.geometryShader()) {
            return -1;
        }

        if(!validateExtensions()) {
            return -1;
        }

        if(surfaceFormat == null || surfacePresentMode == -1) {
            return -1;
        }

        return switch(deviceProperties.deviceType()) {
            case VK_PHYSICAL_DEVICE_TYPE_OTHER -> 100000000;
            case VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU -> 400000000;
            case VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU -> 500000000;
            case VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU -> 300000000;
            case VK_PHYSICAL_DEVICE_TYPE_CPU -> 200000000;
            default -> 0;
        };
    }

    private boolean validateExtensions() {
        try(var stack = MemoryStack.stackPush()) {
            var countPointer = stack.ints(0);
            vkEnumerateDeviceExtensionProperties(device, (ByteBuffer) null, countPointer, null);

            var count = countPointer.get(0);
            var extensions = VkExtensionProperties.calloc(count, stack);
            vkEnumerateDeviceExtensionProperties(device, (ByteBuffer) null, countPointer, extensions);

            var missingExtensions = new HashSet<>(VulkanUtils.DEVICE_EXTENSIONS);
            for (var extension : extensions) {
                missingExtensions.remove(extension.extensionNameString());
            }

            return missingExtensions.isEmpty();
        }
    }

    @NotNull
    public static VulkanPhysicalDevice pick(@NotNull VulkanInstance instance, @NotNull VulkanSurface surface, @NotNull VkWindow window) {
        try(var stack = MemoryStack.stackPush()) {
            var deviceCountPointer = stack.ints(0);
            vkEnumeratePhysicalDevices(instance.handle(), deviceCountPointer, null);

            var deviceCount = deviceCountPointer.get(0);
            var devicesPointer = stack.mallocPointer(deviceCount);
            vkEnumeratePhysicalDevices(instance.handle(), deviceCountPointer, devicesPointer);
            var devices = new ArrayList<VkPhysicalDevice>(deviceCount);
            for(int i = 0; i < deviceCount; i++) {
                devices.add(new VkPhysicalDevice(devicesPointer.get(i), instance.handle()));
            }

            return devices.stream()
                .map((device) -> new VulkanPhysicalDevice(device, surface, window))
                .filter((device) -> {
                    if(device.rank() == -1) {
                        device.close();
                        return false;
                    }
                    return true;
                })
                .max(Comparator.comparingInt(VulkanPhysicalDevice::rank))
                .orElseThrow(() -> new RuntimeException("Failed to find suitable Vulkan device"));
        }
    }

    public int graphicsQueue() {
        return graphicsQueue;
    }

    public int presentQueue() {
        return presentQueue;
    }

    public float maxSamplerAnisotropy() {
        if(deviceFeatures.samplerAnisotropy()) {
            return deviceProperties.limits().maxSamplerAnisotropy();
        } else {
            return Float.NaN;
        }
    }

    @NotNull
    public VkSurfaceCapabilitiesKHR surfaceCapabilities(@NotNull MemoryStack stack) {
        var capabilities = VkSurfaceCapabilitiesKHR.calloc(stack);
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface.handle(), capabilities);
        return capabilities;
    }

    @NotNull
    public VkSurfaceFormatKHR surfaceFormat() {
        assert(surfaceFormat != null);
        return surfaceFormat;
    }

    public int surfacePresentMode() {
        return surfacePresentMode;
    }

    @NotNull
    public VkExtent2D surfaceExtent(@NotNull MemoryStack stack) {
        var extent = VkExtent2D.calloc(stack);
        chooseSwpExtent(extent);
        return extent;
    }

    @Override
    public void close() {
        deviceFeatures.close();
        deviceProperties.close();

        if(surfaceFormat != null) {
            surfaceFormat.free();
        }
    }
}

package net.gudenau.cavegame.renderer.vk;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK10.*;

public final class VulkanLogicalDevice implements AutoCloseable {
    @NotNull
    private final VulkanPhysicalDevice device;
    @NotNull
    private final VkDevice handle;

    @NotNull
    private final VkQueue graphicsQueue;
    @NotNull
    private final VkQueue presentQueue;

    public VulkanLogicalDevice(@NotNull VulkanPhysicalDevice physicalDevice) {
        this.device = physicalDevice;

        try(var stack = MemoryStack.stackPush()) {
            var queueSet = new IntOpenHashSet();
            queueSet.add(physicalDevice.graphicsQueue());
            queueSet.add(physicalDevice.presentQueue());

            var queueCreateInfo = VkDeviceQueueCreateInfo.calloc(queueSet.size(), stack);
            int index = 0; // This sucks
            for(int queue : queueSet) {
                queueCreateInfo.position(index++);
                queueCreateInfo.sType$Default();
                queueCreateInfo.queueFamilyIndex(queue);
                queueCreateInfo.queueCount();
                queueCreateInfo.pQueuePriorities(stack.floats(1));
            }
            queueCreateInfo.position(0);

            var deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack);

            var createInfo = VkDeviceCreateInfo.calloc(stack);
            createInfo.sType$Default();
            createInfo.pQueueCreateInfos(queueCreateInfo);
            createInfo.pEnabledFeatures(deviceFeatures);
            createInfo.ppEnabledExtensionNames(VulkanUtils.packStrings(stack, VulkanUtils.DEVICE_EXTENSIONS));

            var pointer = stack.pointers(0);
            var result = vkCreateDevice(physicalDevice.device(), createInfo, VulkanAllocator.get(), pointer);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create logical Vulkan device: " + VulkanUtils.errorString(result));
            }
            handle = new VkDevice(pointer.get(0), physicalDevice.device(), createInfo);

            var queueMap = new Int2ObjectArrayMap<VkQueue>(queueSet.size());
            for (int queue : queueSet) {
                queueMap.put(queue, getQueue(queue));
            }
            graphicsQueue = queueMap.get(physicalDevice.graphicsQueue());
            presentQueue = queueMap.get(physicalDevice.presentQueue());
        }
    }

    @NotNull
    private VkQueue getQueue(int id) {
        try(var stack = MemoryStack.stackPush()) {
            var pointer = stack.pointers(0);
            vkGetDeviceQueue(handle, id, 0, pointer);
            return new VkQueue(pointer.get(), handle);
        }
    }

    @NotNull
    public VkQueue graphicsQueue() {
        return graphicsQueue;
    }

    @NotNull
    public VkQueue presentQueue() {
        return presentQueue;
    }

    public VulkanPhysicalDevice device() {
        return device;
    }

    @NotNull
    public VkDevice handle() {
        return handle;
    }

    public void waitForIdle() {
        vkDeviceWaitIdle(handle);
    }

    @Override
    public void close() {
        vkDestroyDevice(handle, VulkanAllocator.get());
    }
}

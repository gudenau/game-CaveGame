package net.gudenau.cavegame.renderer.vk;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public final class VulkanCommandPool implements AutoCloseable {
    @NotNull
    private final VulkanLogicalDevice device;
    private final long handle;

    public VulkanCommandPool(@NotNull VulkanPhysicalDevice physicalDevice, @NotNull VulkanLogicalDevice logicalDevice) {
        this.device = logicalDevice;

        try(var stack = MemoryStack.stackPush()) {
            var poolInfo = VkCommandPoolCreateInfo.calloc(stack);
            poolInfo.sType$Default();
            poolInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
            poolInfo.queueFamilyIndex(physicalDevice.graphicsQueue());

            var pointer = stack.longs(0);
            var result = vkCreateCommandPool(logicalDevice.handle(), poolInfo, VulkanAllocator.get(), pointer);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create Vulkan command pool: " + VulkanUtils.errorString(result));
            }
            handle = pointer.get(0);
        }
    }

    public long handle() {
        return handle;
    }

    @Override
    public void close() {
        vkDestroyCommandPool(device.handle(), handle, VulkanAllocator.get());
    }
}

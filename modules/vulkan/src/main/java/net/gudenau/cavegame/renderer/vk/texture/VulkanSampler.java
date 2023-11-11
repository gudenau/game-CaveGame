package net.gudenau.cavegame.renderer.vk.texture;

import net.gudenau.cavegame.renderer.vk.VulkanAllocator;
import net.gudenau.cavegame.renderer.vk.VulkanLogicalDevice;
import net.gudenau.cavegame.renderer.vk.VulkanUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public final class VulkanSampler implements AutoCloseable {
    private final VulkanLogicalDevice device;

    private final long handle;

    public VulkanSampler(VulkanLogicalDevice device, int mipLevels) {
        this.device = device;

        try(var stack = MemoryStack.stackPush()) {
            var createInfo = VkSamplerCreateInfo.calloc(stack);
            createInfo.sType$Default();
            createInfo.magFilter(VK_FILTER_NEAREST);
            createInfo.magFilter(VK_FILTER_LINEAR);
            createInfo.addressModeU(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);
            createInfo.addressModeV(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);
            createInfo.addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);

            var maxAnisotropy = device.device().maxSamplerAnisotropy();
            if(!Float.isNaN(maxAnisotropy)) {
                createInfo.anisotropyEnable(true);
                createInfo.maxAnisotropy(maxAnisotropy);
            } else {
                createInfo.anisotropyEnable(false);
                createInfo.maxAnisotropy(1);
            }

            createInfo.borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK);
            createInfo.unnormalizedCoordinates(false);
            createInfo.compareEnable(false);
            createInfo.compareOp(VK_COMPARE_OP_ALWAYS);
            createInfo.mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR);
            createInfo.mipLodBias(0);
            createInfo.minLod(0);
            createInfo.maxLod(mipLevels);

            var handlePointer = stack.longs(0);
            var result = VK10.vkCreateSampler(device.handle(), createInfo, VulkanAllocator.get(), handlePointer);
            if(result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create image sampler: " + VulkanUtils.errorString(result));
            }
            handle = handlePointer.get(0);
        }
    }

    public long handle() {
        return handle;
    }

    @Override
    public void close() {
        vkDestroySampler(device.handle(), handle, VulkanAllocator.get());
    }
}

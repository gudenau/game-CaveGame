package net.gudenau.cavegame.renderer.vk.texture;

import net.gudenau.cavegame.renderer.texture.PngReader;
import net.gudenau.cavegame.renderer.texture.Texture;
import net.gudenau.cavegame.renderer.vk.*;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK10.*;

//TODO Move Vulkan image stuff into it's own class
public final class VulkanTexture implements Texture {
    private final VulkanLogicalDevice device;

    private final VulkanSampler sampler;
    private final int width;
    private final int height;
    private final int format;

    private final long handle;
    private final VulkanMemory memory;
    private final VulkanImageView imageView;
    private final long size;

    private int layout = VK_IMAGE_LAYOUT_UNDEFINED;

    public VulkanTexture(
        @NotNull VkRenderer renderer,
        @NotNull VulkanSampler sampler,
        @NotNull VkGraphicsBuffer stagingBuffer,
        @NotNull PngReader.Result imageResult
    ) {
        width = imageResult.width();
        height = imageResult.height();
        format = switch(imageResult.format()) {
            case RGBA -> VK_FORMAT_R8G8B8A8_SRGB;
            case RGB -> VK_FORMAT_R8G8B8_SRGB;
            case GRAYSCALE -> VK_FORMAT_R8_SRGB;
        };

        device = renderer.logicalDevice();
        this.sampler = sampler;
        VulkanCommandPool commandPool = renderer.commandPool();

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

            var memoryRequirements = VkMemoryRequirements.calloc(stack);
            vkGetImageMemoryRequirements(device.handle(), handle, memoryRequirements);
            size = memoryRequirements.size();

            memory = new VulkanMemory(device, memoryRequirements, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
            vkBindImageMemory(device.handle(), handle, memory.handle(), 0);

            try(var commandBuffer = new VulkanCommandBuffer(device, commandPool)) {
                commandBuffer.begin();

                transitionLayout(commandBuffer, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
                upload(commandBuffer, stagingBuffer);
                transitionLayout(commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

                commandBuffer.end();
                commandBuffer.submit(device.graphicsQueue());
                vkQueueWaitIdle(device.graphicsQueue());
            }

            imageView = new VulkanImageView(device, handle, format);
        }
    }

    @NotNull
    public VulkanImageView imageView() {
        return imageView;
    }

    @NotNull
    public VulkanSampler sampler() {
        return sampler;
    }

    private record LayoutInfo(int accessMask, int stage) {}
    private static LayoutInfo layoutInfo(int layout) {
        return switch(layout) {
            case VK_IMAGE_LAYOUT_UNDEFINED -> new LayoutInfo(
                0, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT
            );
            case VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL -> new LayoutInfo(
                0, VK_PIPELINE_STAGE_TRANSFER_BIT
            );
            case VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL -> new LayoutInfo(
                VK_ACCESS_SHADER_READ_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
            );
            default -> throw new IllegalArgumentException("Unsupported image layout: " + layout);
        };
    }

    private void transitionLayout(VulkanCommandBuffer commandBuffer, int newLayout) {
        //try(var commandBuffer = new VulkanCommandBuffer(device, commandPool)) {
        //    commandBuffer.begin();
            try(var stack = MemoryStack.stackPush()) { //TODO Move to VulkanCommandBuffer
                var barriers = VkImageMemoryBarrier.calloc(1, stack);
                var barrier = barriers.get(0);
                barrier.sType$Default();
                barrier.oldLayout(layout);
                barrier.newLayout(newLayout);
                barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                barrier.image(handle);
                barrier.subresourceRange().set(
                    VK_IMAGE_ASPECT_COLOR_BIT,
                    0,
                    1,
                    0,
                    1
                );

                var source = layoutInfo(layout);
                var destination = layoutInfo(layout);

                barrier.srcAccessMask(source.accessMask);
                barrier.dstAccessMask(destination.accessMask);

                vkCmdPipelineBarrier(
                    commandBuffer.handle(),
                    source.stage,
                    destination.stage,
                    0,
                    null,
                    null,
                    barriers
                );
            }
        //    commandBuffer.end();
        //    commandBuffer.submit(device.graphicsQueue());
        //    vkQueueWaitIdle(device.graphicsQueue());
        //}

        layout = newLayout;
    }

    private void upload(VulkanCommandBuffer commandBuffer, @NotNull VkGraphicsBuffer buffer) {
        if(buffer.size() != size) {
            throw new IllegalArgumentException("Buffer was the wrong size; expected " + size + " and got " + buffer.size());
        }

        //try(var commandBuffer = new VulkanCommandBuffer(device, commandPool)) {
        //    commandBuffer.begin();
            try(var stack = MemoryStack.stackPush()) {  //TODO Move to VulkanCommandBuffer
                var regions = VkBufferImageCopy.calloc(1, stack);
                var region = regions.get(0);
                region.bufferOffset(0);
                region.bufferRowLength(0);
                region.bufferImageHeight(0);
                region.imageSubresource().set(
                    VK_IMAGE_ASPECT_COLOR_BIT,
                    0,
                    0,
                    1
                );
                region.imageOffset().set(0, 0, 0);
                region.imageExtent().set(
                    width,
                    height,
                    1
                );
                vkCmdCopyBufferToImage(commandBuffer.handle(), buffer.handle(), handle, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, regions);
            }
        //    commandBuffer.end();
        //    commandBuffer.submit(device.graphicsQueue());
        //    vkQueueWaitIdle(device.graphicsQueue());
        //}
    }

    @Override
    public void close() {
        imageView.close();
        vkDestroyImage(device.handle(), handle, VulkanAllocator.get());
        memory.close();
    }
}

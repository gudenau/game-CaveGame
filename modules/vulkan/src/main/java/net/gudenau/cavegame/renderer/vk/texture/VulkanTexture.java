package net.gudenau.cavegame.renderer.vk.texture;

import net.gudenau.cavegame.renderer.texture.NativeTexture;
import net.gudenau.cavegame.renderer.texture.PngReader;
import net.gudenau.cavegame.renderer.texture.Texture;
import net.gudenau.cavegame.renderer.vk.*;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.Set;

import static org.lwjgl.vulkan.VK10.*;

//TODO Move Vulkan image stuff into it's own class
public sealed class VulkanTexture implements Texture permits VulkanAtlasedTexture {
    public enum Flag {
        DISABLE_MIPMAP,
    }

    private final VulkanLogicalDevice device;

    private final VulkanSampler sampler;
    private final int width;
    private final int height;
    private final int format;

    private final VulkanImage image;
    private final VulkanImageView imageView;

    private int layout = VK_IMAGE_LAYOUT_UNDEFINED;

    public VulkanTexture(
        @NotNull VkRenderer renderer,
        @NotNull VulkanTextureManager textureManager,
        @NotNull VkGraphicsBuffer stagingBuffer,
        @NotNull NativeTexture imageResult,
        @NotNull Flag @NotNull ... flags
    ) {
        this(renderer, textureManager, stagingBuffer, imageResult, Set.of(flags));
    }

    public VulkanTexture(
        @NotNull VkRenderer renderer,
        @NotNull VulkanTextureManager textureManager,
        @NotNull VkGraphicsBuffer stagingBuffer,
        @NotNull NativeTexture imageResult,
        @NotNull Set<@NotNull Flag> flags
    ) {
        width = imageResult.width();
        height = imageResult.height();
        format = switch(imageResult.format()) {
            case RGBA -> VK_FORMAT_R8G8B8A8_SRGB;
            case RGB -> VK_FORMAT_R8G8B8_SRGB;
            case GRAYSCALE -> VK_FORMAT_R8_SRGB;
        };

        int mipLevels = Math.max(1, 31 - Integer.numberOfLeadingZeros(Math.max(width, height)));
        if(flags.contains(Flag.DISABLE_MIPMAP)) {
            mipLevels = 1;
        }
        if(mipLevels > 1) {
            try(var stack = MemoryStack.stackPush()) {
                var properties = VkFormatProperties.calloc(stack);
                vkGetPhysicalDeviceFormatProperties(renderer.physicalDevice().device(), format, properties);
                if((properties.optimalTilingFeatures() & VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT) == 0) {
                    mipLevels = 1;
                }
            }
        }

        device = renderer.logicalDevice();
        this.sampler = textureManager.sampler(mipLevels);
        VulkanCommandPool commandPool = renderer.commandPool();

        image = new VulkanImage(
            device,
            width,
            height,
            format,
            VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
            mipLevels
        );

        try(var commandBuffer = new VulkanCommandBuffer(device, commandPool)) {
            commandBuffer.begin();

            transitionLayout(commandBuffer, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
            upload(commandBuffer, stagingBuffer);
            if(mipLevels > 1) {
                generateMipmaps(commandBuffer);
            } else {
                transitionLayout(commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            }

            commandBuffer.end();
            commandBuffer.submit(device.graphicsQueue());
            vkQueueWaitIdle(device.graphicsQueue());
        }

        imageView = new VulkanImageView(device, image);
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
                barrier.image(image.handle());
                barrier.subresourceRange().set(
                    VK_IMAGE_ASPECT_COLOR_BIT,
                    0,
                    image.mipLevels(),
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

    private void generateMipmaps(VulkanCommandBuffer commandBuffer) {
        try(var stack = MemoryStack.stackPush()) {
            var barriers = VkImageMemoryBarrier.calloc(1, stack);
            var barrier = barriers.get(0);
            barrier.sType$Default();
            barrier.image(image.handle());
            barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.subresourceRange().set(
                VK_IMAGE_ASPECT_COLOR_BIT,
                0,
                1,
                0,
                1
            );

            var blits = VkImageBlit.calloc(1, stack);
            var blit = blits.get(0);

            int width = this.width;
            int height = this.height;
            for(int i = 1; i < image.mipLevels(); i++) {
                barrier.subresourceRange().baseMipLevel(i - 1);
                barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
                barrier.newLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
                vkCmdPipelineBarrier(
                    commandBuffer.handle(),
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    0,
                    null,
                    null,
                    barriers
                );

                blit.srcOffsets(0).set(0, 0, 0);
                blit.srcOffsets(1).set(width, height, 1);
                blit.srcSubresource().set(
                    VK_IMAGE_ASPECT_COLOR_BIT,
                    i - 1,
                    0,
                    1
                );
                blit.dstOffsets(0).set(0, 0, 0);
                blit.dstOffsets(1).set(
                    width > 1 ? width / 2 : 1,
                    height > 1 ? height / 2 : 1,
                    1
                );
                blit.dstSubresource().set(
                    VK_IMAGE_ASPECT_COLOR_BIT,
                    i,
                    0,
                    1
                );
                vkCmdBlitImage(
                    commandBuffer.handle(),
                    image.handle(), VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    image.handle(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    blits,
                    VK_FILTER_LINEAR
                );

                barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
                barrier.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
                barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
                vkCmdPipelineBarrier(
                    commandBuffer.handle(),
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                    0,
                    null,
                    null,
                    barriers
                );

                width = Math.max(width / 2, 1);
                height = Math.max(height / 2, 1);
            }

            barrier.subresourceRange().baseMipLevel(image.mipLevels() - 1);
            barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
            barrier.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
            barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
            vkCmdPipelineBarrier(
                commandBuffer.handle(),
                VK_PIPELINE_STAGE_TRANSFER_BIT,
                VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                0,
                null,
                null,
                barriers
            );
        }
    }

    private void upload(VulkanCommandBuffer commandBuffer, @NotNull VkGraphicsBuffer buffer) {
        if(buffer.size() > image.size()) {
            throw new IllegalArgumentException("Buffer was the too large; expected at most " + image.size() + " and got " + buffer.size());
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
                vkCmdCopyBufferToImage(commandBuffer.handle(), buffer.handle(), image.handle(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, regions);
            }
        //    commandBuffer.end();
        //    commandBuffer.submit(device.graphicsQueue());
        //    vkQueueWaitIdle(device.graphicsQueue());
        //}
    }

    @Override
    public void close() {
        imageView.close();
        image.close();
    }
}

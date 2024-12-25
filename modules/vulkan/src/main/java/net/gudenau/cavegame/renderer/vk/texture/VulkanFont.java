package net.gudenau.cavegame.renderer.vk.texture;

import net.gudenau.cavegame.renderer.texture.Font;
import net.gudenau.cavegame.renderer.texture.NativeTexture;
import net.gudenau.cavegame.renderer.texture.PngReader;
import net.gudenau.cavegame.renderer.vk.VkGraphicsBuffer;
import net.gudenau.cavegame.renderer.vk.VkRenderer;
import org.jetbrains.annotations.NotNull;

public final class VulkanFont extends VulkanAtlasedTexture<Integer> implements Font {
    public VulkanFont(@NotNull VkRenderer renderer, @NotNull VulkanTextureManager textureManager, @NotNull VkGraphicsBuffer stagingBuffer, @NotNull NativeTexture imageResult) {
        super(renderer, textureManager, stagingBuffer, imageResult);
    }
}

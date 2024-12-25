package net.gudenau.cavegame.renderer.vk.texture;

import net.gudenau.cavegame.renderer.texture.AtlasedTexture;
import net.gudenau.cavegame.renderer.texture.NativeTexture;
import net.gudenau.cavegame.renderer.texture.PngReader;
import net.gudenau.cavegame.renderer.texture.Sprite;
import net.gudenau.cavegame.renderer.vk.VkGraphicsBuffer;
import net.gudenau.cavegame.renderer.vk.VkRenderer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.stream.Stream;

public sealed class VulkanAtlasedTexture<K> extends VulkanTexture implements AtlasedTexture<K> permits VulkanFont {
    public VulkanAtlasedTexture(@NotNull VkRenderer renderer, @NotNull VulkanTextureManager textureManager, @NotNull VkGraphicsBuffer stagingBuffer, @NotNull NativeTexture imageResult) {
        super(renderer, textureManager, stagingBuffer, imageResult, Flag.DISABLE_MIPMAP);
    }

    @Override
    @NotNull
    public Optional<Sprite> sprite(@NotNull K key) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Stream<Sprite> sprites() {
        throw new UnsupportedOperationException();
    }
}

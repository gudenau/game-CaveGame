package net.gudenau.cavegame.renderer.vk.texture;

import net.gudenau.cavegame.renderer.BufferType;
import net.gudenau.cavegame.renderer.texture.NativeTexture;
import net.gudenau.cavegame.renderer.texture.SpriteAtlas;
import net.gudenau.cavegame.renderer.texture.TextureFormat;
import net.gudenau.cavegame.renderer.vk.VkRenderer;
import net.gudenau.cavegame.resource.Identifier;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;

public final class VulkanSpriteAtlas extends VulkanAtlasedTexture<Identifier> implements SpriteAtlas {
    public VulkanSpriteAtlas(
        @NotNull VkRenderer renderer,
        @NotNull VulkanTextureManager textureManager,
        @NotNull TextureFormat format
    ) {
        var texture = NativeTexture.create(1, 1, format);
        var pixels = texture.pixels();
        MemoryUtil.memSet(pixels, 0);

        var stagingBuffer = renderer.createBuffer(BufferType.STAGING, pixels.remaining());
        stagingBuffer.upload(pixels);

        super(renderer, textureManager, stagingBuffer, texture);

        stagingBuffer.close();
        texture.close();
    }
}

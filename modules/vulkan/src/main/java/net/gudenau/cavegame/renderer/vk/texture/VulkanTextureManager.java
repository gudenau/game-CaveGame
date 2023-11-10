package net.gudenau.cavegame.renderer.vk.texture;

import net.gudenau.cavegame.renderer.BufferType;
import net.gudenau.cavegame.renderer.texture.PngReader;
import net.gudenau.cavegame.renderer.texture.Texture;
import net.gudenau.cavegame.renderer.texture.TextureFormat;
import net.gudenau.cavegame.renderer.texture.TextureManager;
import net.gudenau.cavegame.renderer.vk.VkGraphicsBuffer;
import net.gudenau.cavegame.renderer.vk.VkRenderer;
import net.gudenau.cavegame.resource.Identifier;
import net.gudenau.cavegame.resource.ResourceLoader;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class VulkanTextureManager implements TextureManager, AutoCloseable {
    private final VkRenderer renderer;
    private final VulkanSampler sampler;

    private final Map<Identifier, VulkanTexture> textures = new HashMap<>();

    public VulkanTextureManager(VkRenderer renderer) {
        this.renderer = renderer;

        sampler = new VulkanSampler(renderer.logicalDevice());
    }

    public void close() {
        textures.values().forEach(VulkanTexture::close);
        sampler.close();
    }

    @NotNull
    @Override
    public Texture loadTexture(@NotNull Identifier identifier, @NotNull TextureFormat format) throws IOException {
        PngReader.Result imageResult;
        var imageFileBuffer = ResourceLoader.buffer(identifier.normalize("texture", ".png"));
        try {
            imageResult = PngReader.read(imageFileBuffer, format);
        } finally {
            MemoryUtil.memFree(imageFileBuffer);
        }
        var pixels = imageResult.pixels();
        VkGraphicsBuffer stagingBuffer;
        try {
            stagingBuffer = renderer.createBuffer(BufferType.STAGING, pixels.remaining());
            stagingBuffer.upload(pixels);
        } finally {
            MemoryUtil.memFree(pixels);
        }
        VulkanTexture texture;
        try {
            texture = new VulkanTexture(renderer, sampler, stagingBuffer, imageResult);
        } finally {
            stagingBuffer.close();
        }
        var existing = textures.put(identifier, texture);
        if(existing != null) {
            existing.close();
        }
        return texture;
    }

    public Optional<VulkanTexture> getTexture(Identifier identifier) {
        return Optional.ofNullable(textures.get(identifier));
    }
}

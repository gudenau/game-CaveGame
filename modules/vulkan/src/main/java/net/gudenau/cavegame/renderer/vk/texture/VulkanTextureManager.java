package net.gudenau.cavegame.renderer.vk.texture;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.gudenau.cavegame.renderer.BufferType;
import net.gudenau.cavegame.renderer.texture.PngReader;
import net.gudenau.cavegame.renderer.texture.Texture;
import net.gudenau.cavegame.renderer.texture.TextureFormat;
import net.gudenau.cavegame.renderer.texture.TextureManager;
import net.gudenau.cavegame.renderer.vk.VkGraphicsBuffer;
import net.gudenau.cavegame.renderer.vk.VkRenderer;
import net.gudenau.cavegame.resource.Identifier;
import net.gudenau.cavegame.resource.ResourceLoader;
import net.gudenau.cavegame.util.SharedLock;
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
    private final SharedLock samplers$lock = new SharedLock();
    private final Int2ObjectMap<VulkanSampler> samplers = new Int2ObjectOpenHashMap<>();

    private final Map<Identifier, VulkanTexture> textures = new HashMap<>();

    public VulkanTextureManager(VkRenderer renderer) {
        this.renderer = renderer;
    }

    public void close() {
        textures.values().forEach(VulkanTexture::close);
        samplers.values().forEach(VulkanSampler::close);
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
            texture = new VulkanTexture(renderer, this, stagingBuffer, imageResult);
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

    public VulkanSampler sampler(int mipLevels) {
        var sampler = samplers$lock.read(() -> samplers.get(mipLevels));
        if(sampler != null) {
            return sampler;
        }
        return samplers$lock.write(() -> samplers.computeIfAbsent(mipLevels, (level) -> new VulkanSampler(renderer.logicalDevice(), mipLevels)));
    }
}

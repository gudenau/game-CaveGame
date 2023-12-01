package net.gudenau.cavegame.renderer.gl;

import net.gudenau.cavegame.renderer.texture.PngReader;
import net.gudenau.cavegame.renderer.texture.Texture;
import net.gudenau.cavegame.renderer.texture.TextureFormat;
import net.gudenau.cavegame.renderer.texture.TextureManager;
import net.gudenau.cavegame.resource.Identifier;
import net.gudenau.cavegame.resource.ResourceLoader;
import net.gudenau.cavegame.util.SharedLock;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class GlTextureManager implements AutoCloseable, TextureManager {
    private final SharedLock textures$lock = new SharedLock();
    private final Map<Identifier, Texture> textures = new HashMap<>();
    private final GlExecutor executor;

    public GlTextureManager(@NotNull GlExecutor executor) {
        this.executor = executor;
    }

    @Override
    public @NotNull Texture loadTexture(@NotNull Identifier identifier, @NotNull TextureFormat format) throws IOException {
        var pngBuffer = ResourceLoader.buffer(identifier.normalize("texture", ".png"));
        PngReader.Result result;
        try {
            result = PngReader.read(pngBuffer, format);
        } finally {
            MemoryUtil.memFree(pngBuffer);
        }
        GlTexture texture;
        try {
            texture = executor.get((state) -> new GlTexture(state, result));
        } finally {
            result.close();
        }
        return textures$lock.write(() -> {
            var existing = textures.putIfAbsent(identifier, texture);
            if(existing != null) {
                texture.close();
                return existing;
            } else {
                return texture;
            }
        });
    }

    @Override
    public void close() {
        textures$lock.write(() -> {
            textures.values().forEach(Texture::close);
            textures.clear();
        });
    }
}

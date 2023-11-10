package net.gudenau.cavegame.renderer;

import net.gudenau.cavegame.renderer.shader.Shader;
import net.gudenau.cavegame.renderer.texture.Texture;
import net.gudenau.cavegame.renderer.texture.TextureManager;
import net.gudenau.cavegame.resource.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface Renderer extends AutoCloseable {
    @Override void close();

    void draw();

    Shader loadShader(@NotNull Identifier identifier, @NotNull Map<String, Texture> textures);

    @NotNull
    GraphicsBuffer createBuffer(@NotNull BufferType type, int size);

    void drawBuffer(int vertexCount, @NotNull GraphicsBuffer vertexBuffer, @Nullable GraphicsBuffer indexBuffer);

    void begin();

    void waitForIdle();

    @NotNull TextureManager textureManager();
}

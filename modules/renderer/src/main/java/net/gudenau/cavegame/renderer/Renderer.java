package net.gudenau.cavegame.renderer;

import net.gudenau.cavegame.renderer.shader.Shader;
import net.gudenau.cavegame.resource.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface Renderer extends AutoCloseable {
    @Override void close();

    void draw();

    Shader loadShader(@NotNull Identifier basic);

    @NotNull
    GraphicsBuffer createBuffer(@NotNull BufferType type, int size);

    void drawBuffer(@NotNull GraphicsBuffer buffer);

    void begin();

    void waitForIdle();
}

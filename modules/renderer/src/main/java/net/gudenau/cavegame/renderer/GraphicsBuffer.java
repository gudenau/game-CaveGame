package net.gudenau.cavegame.renderer;

import net.gudenau.cavegame.renderer.shader.Shader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

public interface GraphicsBuffer extends AutoCloseable {
    void upload(@NotNull ByteBuffer data);
    @Nullable Shader shader();

    @Override void close();
}

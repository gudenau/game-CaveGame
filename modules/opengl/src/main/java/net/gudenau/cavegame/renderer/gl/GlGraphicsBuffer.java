package net.gudenau.cavegame.renderer.gl;

import net.gudenau.cavegame.renderer.BufferType;
import net.gudenau.cavegame.renderer.GraphicsBuffer;
import net.gudenau.cavegame.renderer.gl.shader.GlProgram;
import net.gudenau.cavegame.renderer.shader.Shader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL33C.*;

public final class GlGraphicsBuffer implements GraphicsBuffer {
    private final GlExecutor executor;
    private final int handle;
    private final BufferType type;
    private final int size;

    private GlProgram shader;

    public GlGraphicsBuffer(@NotNull GlExecutor executor, @NotNull BufferType type, int size) {
        this.executor = executor;
        handle = executor.get((state) -> glGenBuffers());
        this.type = type;
        this.size = size;
    }

    @Override
    public void upload(@NotNull ByteBuffer data) {
        executor.run((state) -> {
            state.bindBuffer(handle);
            var limit = data.limit();
            try {
                data.limit(size + data.position());
                glBufferData(GL_ARRAY_BUFFER, data, switch(type) {
                    case VERTEX, UNIFORM, INDEX -> GL_STATIC_DRAW;
                    case STAGING -> GL_STREAM_DRAW;
                });
            } finally {
                state.bindBuffer(0);
                data.limit(limit);
            }
        });
    }

    @Override
    public @Nullable Shader shader() {
        return shader;
    }

    @Override
    public void close() {
        glDeleteBuffers(handle);
    }

    public void shader(@NotNull GlProgram shader) {
        this.shader = shader;
    }
}

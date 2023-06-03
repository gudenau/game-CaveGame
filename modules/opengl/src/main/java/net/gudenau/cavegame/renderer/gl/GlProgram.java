package net.gudenau.cavegame.renderer.gl;

import org.jetbrains.annotations.NotNull;

import static org.lwjgl.opengl.GL33.*;

public final class GlProgram implements AutoCloseable {
    private final int handle;

    public GlProgram(@NotNull GlShader @NotNull ... shaders) {
        handle = glCreateProgram();
        for (var shader : shaders) {
            glAttachShader(handle, shader.handle());
        }
        glLinkProgram(handle);

        if(glGetProgrami(handle, GL_LINK_STATUS) != GL_TRUE) {
            var error = glGetProgramInfoLog(handle);
            throw new RuntimeException("Failed to link program: " + error);
        }
    }

    public void bind() {
        GlState.get().bindProgram(handle);
    }

    public void release() {
        var state = GlState.get();
        if(state.boundProgram() == handle) {
            state.bindProgram(0);
        }
    }

    @Override
    public void close() {
        glDeleteProgram(handle);
    }
}

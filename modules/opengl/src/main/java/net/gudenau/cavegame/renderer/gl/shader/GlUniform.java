package net.gudenau.cavegame.renderer.gl.shader;

import net.gudenau.cavegame.renderer.shader.AttributeType;
import net.gudenau.cavegame.renderer.shader.Uniform;
import net.gudenau.cavegame.renderer.shader.UniformUsage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public record GlUniform(
    @NotNull String name,
    @NotNull AttributeType type,
    int count,
    int stride,
    int location,
    @Nullable UniformUsage usage,
    int binding
) implements Uniform {
    @Override
    public void upload(@NotNull Consumer<ByteBuffer> consumer) {
        throw new UnsupportedOperationException();
    }
}

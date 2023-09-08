package net.gudenau.cavegame.renderer;

import net.gudenau.cavegame.renderer.internal.BufferBuilderImpl;
import net.gudenau.cavegame.renderer.shader.Shader;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.function.Function;

public interface BufferBuilder {
    @NotNull
    @Contract("_, _ -> new")
    static BufferBuilder create(@NotNull Shader shader, @NotNull Function<ByteBuffer, GraphicsBuffer> factory) {
        return new BufferBuilderImpl(shader, factory);
    }

    @Contract("_, _, _ -> this")
    @NotNull
    BufferBuilder position(float x, float y, float z);

    @Contract("_, _ -> this")
    @NotNull
    default BufferBuilder position(float x, float y) {
        return position(x, y, 0);
    }

    @Contract("_, _, _, _ -> this")
    @NotNull
    BufferBuilder color(float r, float g, float b, float a);

    @Contract("_, _, _ -> this")
    @NotNull
    default BufferBuilder color(float r, float g, float b) {
        return color(r, g, b, 1);
    }

    @Contract("-> this")
    @NotNull
    BufferBuilder next();

    @NotNull GraphicsBuffer build();
}

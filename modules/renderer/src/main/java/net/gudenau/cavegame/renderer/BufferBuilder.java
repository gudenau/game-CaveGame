package net.gudenau.cavegame.renderer;

import net.gudenau.cavegame.renderer.internal.BufferBuilderImpl;
import net.gudenau.cavegame.renderer.shader.Shader;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface BufferBuilder {
    @NotNull
    @Contract("_, _ -> new")
    static BufferBuilder create(@NotNull Shader shader, @NotNull BiFunction<ByteBuffer, BufferType, GraphicsBuffer> factory) {
        return new BufferBuilderImpl(shader, factory);
    }

    @Contract("_, _, _ -> this")
    @NotNull
    BufferBuilder position(float x, float y, float z);

    @Contract("_ -> this")
    @NotNull
    default BufferBuilder position(@NotNull Vector3f vector) {
        return position(vector.x, vector.y, vector.z);
    }

    @Contract("_, _ -> this")
    @NotNull
    default BufferBuilder position(float x, float y) {
        return position(x, y, 0);
    }

    @Contract("_ -> this")
    @NotNull
    default BufferBuilder position(@NotNull Vector2f vector) {
        return position(vector.x, vector.y);
    }

    @Contract("_, _, _, _ -> this")
    @NotNull
    BufferBuilder color(float r, float g, float b, float a);

    @Contract("_ -> this")
    @NotNull
    default BufferBuilder color(@NotNull Vector4f vector) {
        return color(vector.x, vector.y, vector.z, vector.w);
    }

    @Contract("_, _, _ -> this")
    @NotNull
    default BufferBuilder color(float r, float g, float b) {
        return color(r, g, b, 1);
    }

    @Contract("_ -> this")
    @NotNull
    default BufferBuilder color(@NotNull Vector3f vector) {
        return color(vector.x, vector.y, vector.z);
    }

    @Contract("_, _ -> this")
    @NotNull
    BufferBuilder textureCoord(float u, float v);

    @Contract("_ -> this")
    @NotNull
    default BufferBuilder textureCoord(@NotNull Vector2f vector) {
        return textureCoord(vector.x, vector.y);
    }

    @Contract("-> this")
    @NotNull
    BufferBuilder next();

    @NotNull Map<BufferType, GraphicsBuffer> build();

    int vertexCount();
}

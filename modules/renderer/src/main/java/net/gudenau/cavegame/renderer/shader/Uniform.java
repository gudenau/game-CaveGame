package net.gudenau.cavegame.renderer.shader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public interface Uniform {
    @NotNull String name();
    @NotNull AttributeType type();
    int count();
    int stride();
    int location();
    @Nullable UniformUsage usage();

    void upload(@NotNull Consumer<ByteBuffer> consumer);
}

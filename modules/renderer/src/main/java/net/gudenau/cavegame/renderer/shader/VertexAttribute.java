package net.gudenau.cavegame.renderer.shader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface VertexAttribute {
    @NotNull String name();
    @NotNull AttributeType type();
    int count();
    int stride();
    int location();
    @Nullable AttributeUsage usage();
    int offset();
}

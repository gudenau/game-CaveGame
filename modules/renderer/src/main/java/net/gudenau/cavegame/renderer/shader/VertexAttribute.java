package net.gudenau.cavegame.renderer.shader;

import org.jetbrains.annotations.NotNull;

public interface VertexAttribute {
    @NotNull String name();
    @NotNull AttributeType type();
    int count();
    int stride();
    int location();
    @NotNull AttributeUsage usage();
    int offset();
}

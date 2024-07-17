package net.gudenau.cavegame.renderer.shader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Uniform {
    @NotNull String name();
    @NotNull AttributeType type();
    int count();
    int stride();
    int location();
    @Nullable UniformUsage usage();
    @NotNull ShaderType shader();
}

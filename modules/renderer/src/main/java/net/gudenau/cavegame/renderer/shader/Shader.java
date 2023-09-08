package net.gudenau.cavegame.renderer.shader;

import net.gudenau.cavegame.renderer.BufferBuilder;
import org.jetbrains.annotations.NotNull;

public interface Shader extends AutoCloseable {
    @Override void close();

    @NotNull BufferBuilder builder();

    VertexFormat format();

    record Attribute(
        
    ) {}
}

package net.gudenau.cavegame.renderer.shader;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public interface VertexFormat {
    @NotNull
    List<@NotNull VertexAttribute> attributes();

    @NotNull
    Optional<VertexAttribute> color();

    @NotNull
    Optional<VertexAttribute> position();

    int stride();
}

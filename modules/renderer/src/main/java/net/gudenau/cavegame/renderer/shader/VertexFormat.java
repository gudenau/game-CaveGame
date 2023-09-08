package net.gudenau.cavegame.renderer.shader;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public interface VertexFormat {
    @NotNull
    Map<@NotNull AttributeUsage, @NotNull VertexAttribute> attributes();

    @NotNull
    default Optional<VertexAttribute> color() {
        return Optional.ofNullable(attributes().get(AttributeUsage.COLOR));
    }

    @NotNull
    default Optional<VertexAttribute> position() {
        return Optional.ofNullable(attributes().get(AttributeUsage.POSITION));
    }

    int stride();
}

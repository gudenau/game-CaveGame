package net.gudenau.cavegame.renderer;

import net.gudenau.cavegame.resource.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface Renderer extends AutoCloseable {
    @Override void close();

    void draw();

    Optional<Shader> loadShader(@NotNull Identifier basic);
}

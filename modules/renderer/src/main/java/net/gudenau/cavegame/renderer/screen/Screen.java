package net.gudenau.cavegame.renderer.screen;

import net.gudenau.cavegame.renderer.Renderer;
import org.jetbrains.annotations.NotNull;

public interface Screen extends AutoCloseable {
    void draw(@NotNull Renderer renderer);

    @Override
    default void close() {}
}

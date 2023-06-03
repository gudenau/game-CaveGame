package net.gudenau.cavegame.renderer.internal;

import net.gudenau.cavegame.renderer.Renderer;
import net.gudenau.cavegame.renderer.Window;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public record MergedRenderer(
    @NotNull List<@NotNull Renderer> renderers
) implements Renderer {
    @Override
    public void close() {
        renderers.forEach(Renderer::close);
    }

    @Override
    public void draw() {
        renderers.forEach(Renderer::draw);
    }
}

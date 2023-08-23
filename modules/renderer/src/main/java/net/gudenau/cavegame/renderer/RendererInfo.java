package net.gudenau.cavegame.renderer;

import net.gudenau.cavegame.renderer.internal.RendererInfoImpl;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public interface RendererInfo {
    @NotNull
    static List<@NotNull RendererInfo> availableRenderers() {
        return RendererInfoImpl.availableRenderers();
    }

    boolean supported();
    @NotNull String name();

    @NotNull Renderer createRenderer(@NotNull Window window);

    @NotNull Window createWindow(@NotNull String title, int width, int height);
}

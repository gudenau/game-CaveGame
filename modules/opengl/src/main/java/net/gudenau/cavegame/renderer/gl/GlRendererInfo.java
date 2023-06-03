package net.gudenau.cavegame.renderer.gl;

import net.gudenau.cavegame.renderer.Renderer;
import net.gudenau.cavegame.renderer.RendererInfo;
import net.gudenau.cavegame.renderer.Window;
import org.jetbrains.annotations.NotNull;

public final class GlRendererInfo implements RendererInfo {
    @Override
    public boolean supported() {
        return true;
    }

    @Override
    @NotNull
    public String name() {
        return "CaveGameGl";
    }

    @Override
    @NotNull
    public Renderer createRenderer(@NotNull Window window) {
        return new GlRenderer(window);
    }

    @Override
    @NotNull
    public Window createWindow(@NotNull String title, int width, int height) {
        return new GlWindow(null, title, width, height);
    }
}

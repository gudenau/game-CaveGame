package net.gudenau.cavegame.renderer.vk;

import net.gudenau.cavegame.renderer.Renderer;
import net.gudenau.cavegame.renderer.RendererInfo;
import net.gudenau.cavegame.renderer.Window;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class VkRendererInfo implements RendererInfo {
    @Override
    public boolean supported() {
        return true;
    }

    @Override
    @NotNull
    public String name() {
        return "CaveGameVk";
    }

    @Override
    @NotNull
    public Renderer createRenderer(@NotNull Window window) {
        if(!(window instanceof VkWindow vkWindow)) {
            throw new IllegalArgumentException("Window " + window + " was not a VkWindow");
        }
        return new VkRenderer(vkWindow);
    }

    @Override
    @NotNull
    public Window createWindow(@NotNull String title, int width, int height) {
        return new VkWindow(title, width, height);
    }
}

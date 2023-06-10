package net.gudenau.cavegame.renderer.internal;

import net.gudenau.cavegame.renderer.RendererInfo;
import net.gudenau.cavegame.renderer.Window;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public record MergedWindow(
    @NotNull Map<@NotNull RendererInfo, @NotNull Window> windows
) implements Window {
    public MergedWindow {
        var state = new Object() {
            int x = 0;
        };
        windows.entrySet().stream()
            .sorted((a, b) -> a.getKey().name().compareToIgnoreCase(b.getKey().name()))
            .map(Map.Entry::getValue)
            .forEachOrdered((window) -> {
                window.position(state.x, 0);
                state.x += window.size().width();
            });
    }

    @Override
    public void visible(boolean visible) {
        windows.values().forEach((window) -> window.visible(visible));
    }

    @Override
    public boolean visible() {
        return windows.values().stream().allMatch(Window::visible);
    }

    @Override
    public boolean closeRequested() {
        return windows.values().stream().anyMatch(Window::closeRequested);
    }

    @Override
    public void bind() {
        windows.values().forEach(Window::bind);
    }

    @Override
    public void release() {
        windows.values().forEach(Window::release);
    }

    @Override
    public void flip() {
        windows.values().forEach(Window::flip);
    }

    @Override
    public void close() {
        windows.values().forEach(Window::close);
    }

    @Override
    public void position(int x, int y) {
        windows.values().forEach((window) -> window.position(x, y));
    }

    @Override
    @NotNull
    public Size size() {
        var state = new Object() {
            int width = 1;
            int height = 1;
        };
        windows.values().forEach((window) -> {
            var size = window.size();
            state.width = Math.max(state.width, size.width());
            state.height = Math.max(state.height, size.height());
        });
        return new Size(state.width, state.height);
    }

    @Override
    public void resizeCallback(@Nullable ResizeCallback callback) {
        //TODO I have no idea how one would implement this correctly...
    }
}

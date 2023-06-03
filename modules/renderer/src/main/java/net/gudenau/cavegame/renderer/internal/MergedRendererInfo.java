package net.gudenau.cavegame.renderer.internal;

import net.gudenau.cavegame.renderer.Renderer;
import net.gudenau.cavegame.renderer.RendererInfo;
import net.gudenau.cavegame.renderer.Window;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public record MergedRendererInfo(
    @NotNull String name,
    @NotNull List<@NotNull RendererInfo> rendererInfo
) implements RendererInfo {
    @Override
    public boolean supported() {
        return true;
    }

    @Override
    @NotNull
    public Window createWindow(@NotNull String title, int width, int height) {
        return new MergedWindow(rendererInfo.stream().collect(Collectors.toUnmodifiableMap(
            Function.identity(),
            (renderer) -> renderer.createWindow(title, width, height)
        )));
    }

    @Override
    @NotNull
    public Renderer createRenderer(@NotNull Window window) {
        if(!(window instanceof MergedWindow mergedWindow)) {
            throw new IllegalArgumentException("window " + window + " was not a MergedWindow");
        }

        return new MergedRenderer(mergedWindow.windows().entrySet().stream()
            .map((entry) -> entry.getKey().createRenderer(entry.getValue()))
            .toList()
        );
    }
}

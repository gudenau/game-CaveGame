package net.gudenau.cavegame.gui.drawing;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Font {
    record Bounds(int width, int height) {}

    @NotNull Bounds stringBounds(@NotNull String text);

    int ascent();

    @NotNull
    default TextMetrics metrics(@NotNull String text) {
        return TextMetrics.of(this, text);
    }
}

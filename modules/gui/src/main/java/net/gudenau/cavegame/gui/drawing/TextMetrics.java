package net.gudenau.cavegame.gui.drawing;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record TextMetrics(int ascent, int width, int height) {
    public static final TextMetrics BLANK = new TextMetrics(0, 0, 0);

    @NotNull
    public static TextMetrics of(@NotNull Font font, @NotNull String text) {
        if(text.isBlank()) {
            return new TextMetrics(0, 0, 0);
        }

        var bounds = text.lines()
            .map(font::stringBounds)
            .reduce((a, b) -> new Font.Bounds(
                Math.max(a.width(), b.width()),
                a.height() + b.height()
            ))
            .orElse(null);

        // Shouldn't be hit, but we need to be sure
        if(bounds == null) {
            return BLANK;
        }

        return new TextMetrics(font.ascent(), bounds.width(), bounds.height());
    }
}

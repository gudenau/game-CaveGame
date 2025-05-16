package net.gudenau.cavegame.gui.drawing;

import org.jetbrains.annotations.NotNull;

/// Draws text to the screen and measures the sizes of text.
public interface Font {
    /// Gets the bounds of a line of text.
    ///
    /// @param line The text to get the bounds of
    /// @return A new {@link TextMetrics} instance for the line of text
    @NotNull TextMetrics lineMetrics(@NotNull String line);

    /// Gets the ascent of this font, the Y offset that text needs to be rendered at for proper alignment.
    ///
    /// @return The ascent of this font
    int ascent();

    /// Gets the metrics for some text.
    ///
    /// @param text The text to get metrics for
    /// @return A new {@link TextMetrics} or {@link TextMetrics#BLANK} on an empty string
    @NotNull
    default TextMetrics metrics(@NotNull String text) {
        if(text.isBlank()) {
            return new TextMetrics(0, 0, 0);
        }

        var bounds = text.lines()
            .map(this::lineMetrics)
            .reduce((a, b) -> new TextMetrics(
                0,
                Math.max(a.width(), b.width()),
                a.height() + b.height()
            ))
            .orElse(null);

        // Shouldn't be hit, but we need to be sure
        if(bounds == null) {
            return TextMetrics.BLANK;
        }

        return new TextMetrics(ascent(), bounds.width(), bounds.height());
    }
}

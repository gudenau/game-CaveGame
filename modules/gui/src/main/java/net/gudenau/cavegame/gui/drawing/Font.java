package net.gudenau.cavegame.gui.drawing;

import org.jetbrains.annotations.NotNull;

/// Draws text to the screen and measures the sizes of text.
public interface Font {
    //FIXME Remove this in favor of TextMetrics
    @Deprecated(forRemoval = true)
    record Bounds(int width, int height) {}

    /// Gets the bounds of a line of text.
    ///
    /// @param line The text to get the bounds of
    /// @return A new {@link Bounds} instance for the line of text
    @NotNull Bounds stringBounds(@NotNull String line);

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
        return TextMetrics.of(this, text);
    }
}

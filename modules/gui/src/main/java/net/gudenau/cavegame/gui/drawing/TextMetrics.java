package net.gudenau.cavegame.gui.drawing;

import org.jetbrains.annotations.NotNull;

/// The size and other information about a block of text required to render it.
///
/// @param ascent The Y offset to start drawing text at
/// @param width The width of the block of text
/// @param height The height of the block of text
public record TextMetrics(int ascent, int width, int height) {
    /// A "blank" instance with 0s for all values.
    public static final TextMetrics BLANK = new TextMetrics(0, 0, 0);

    /// Calculates the font metrics of some text.
    ///
    /// @param font The font that will be used to draw the text
    /// @param text The text that will be drawn
    /// @return A new {@link TextMetrics} instance or {@link #BLANK} on an empty string
    //TODO Move this to Font
    @Deprecated(forRemoval = true)
    @NotNull
    public static TextMetrics of(@NotNull Font font, @NotNull String text) {
        return font.metrics(text);
    }
}

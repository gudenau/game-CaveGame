package net.gudenau.cavegame.gui.drawing;

import org.jetbrains.annotations.NotNull;

/// An interface for something that can be drawn to the screen via a {@link DrawContext}.
public interface Drawable {
    /// Draws this object to the screen using the provided {@link DrawContext}
    ///
    /// @param context The context to use to draw this object
    void draw(@NotNull DrawContext context);
}

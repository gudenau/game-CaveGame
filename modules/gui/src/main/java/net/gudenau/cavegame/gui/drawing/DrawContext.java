package net.gudenau.cavegame.gui.drawing;

import net.gudenau.cavegame.resource.Identifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/// The primary interface for GUIs to draw to the screen.
public interface DrawContext {
    /// An entry into the scissor stack.
    interface StackEntry extends AutoCloseable {
        @Override void close();
    }

    /// The width of the drawable area.
    int width();

    /// The height of the drawable area.
    int height();

    /// Creates a new "scissor" entry and pushes it to the internal scissor stack. This causes all new rendering to
    /// occur in the rectangle specified by the arguments. Calling {@link StackEntry#close()} of the returned entry will
    /// pop it off of the stack and return the drawable area to what it was before calling scissor.
    ///
    /// The intended use case looks something like this:
    /// ```Java
    /// try(var _ = context.scissor(10, 10, 20, 20)) {
    ///     drawStuff(context);
    /// }
    /// ```
    ///
    /// @param x The X position of the new drawing bounds
    /// @param y The Y position of the new drawing bounds
    /// @param width The width of the new drawing bounds
    /// @param height The height of the new drawing bounds
    /// @return The new {@link StackEntry}
    @Contract("_, _, _, _ -> new")
    @NotNull StackEntry scissor(int x, int y, int width, int height);

    /// Draws a simple filled rectangle to the screen.
    ///
    /// @param x The X position of the rectangle
    /// @param y The Y position of the rectangle
    /// @param width The width of the rectangle
    /// @param height The height of the rectangle
    /// @param color The color of the rectangle in 0xAARRGGBB format.
    void drawRectangle(int x, int y, int width, int height, int color);

    /// Draws text to the screen.
    ///
    /// @param x The X position of the text
    /// @param y The Y position of the text
    /// @param color The color of the text in 0xAARRGGBB format.
    void drawText(int x, int y, @NotNull String text, int color);

    /// Draws a graphic to the screen.
    ///
    /// @param identifier The identifier of the image
    /// @param x The X position of the image
    /// @param y The Y position of the image
    /// @param width The width of the image
    /// @param height The height of the image
    /// @param u The U coordinate of the image
    /// @param v The V coordinate of the image
    /// @param uWidth The U width of the image
    /// @param vHeight The V height of the image
    /// @param textureWidth The expected width of the texture
    /// @param textureHeight The expected height of the texture
    void drawImage(@NotNull Identifier identifier, int x, int y, int width, int height, int u, int v, int uWidth, int vHeight, int textureWidth, int textureHeight);

    /// Draws a graphic to the screen.
    ///
    /// Equivalent to calling
    /// `drawImage(identifier, x, y, width, height, u, v, width, height, textureWidth, textureHeight)`
    ///
    /// @param identifier The identifier of the image
    /// @param x The X position of the image
    /// @param y The Y position of the image
    /// @param width The width of the image
    /// @param height The height of the image
    /// @param u The U coordinate of the image
    /// @param v The V coordinate of the image
    /// @param textureWidth The expected width of the texture
    /// @param textureHeight The expected height of the texture
    default void drawImage(@NotNull Identifier identifier, int x, int y, int width, int height, int u, int v, int textureWidth, int textureHeight) {
        drawImage(identifier, x, y, width, height, u, v, width, height, textureWidth, textureHeight);
    }
}

package net.gudenau.cavegame.gui.drawing;

import net.gudenau.cavegame.resource.Identifier;
import org.jetbrains.annotations.NotNull;

/// A basic 3x3 graphical component. This is used to draw things like buttons, where they have a variable size but a
/// very small graphic associated with them.
///
/// The image layout should look something like this:
/// +----+----+----+
/// | UL | UM | UR |
/// +----+----+----+
/// | ML | M  | MR |
/// +----+----+----+
/// | LL | LM | LR |
/// +----+----+----+
///
/// Where UL is the upper-left corner of the graphic, UM is the upper middle edge of the graphic, UR is the upper right
/// of the graphic, etc. The sides and middle get stretched to fill in the space the graphic occupies while the corners
/// remain the same size.
public final class ThreeByThree implements Drawable {
    @NotNull
    private final Identifier identifier;
    private final int width;
    private final int height;
    private final int xBorder;
    private final int yBorder;

    /// Constructs a new 3x3 graphic.
    ///
    /// @param identifier The {@link Identifier} for the texture to use when rendering
    /// @param texWidth The expected width of the texture
    /// @param texHeight The expected height of the texture
    /// @param xBorder The width of the corners
    /// @param yBorder The height of the corners
    public ThreeByThree(@NotNull Identifier identifier, int texWidth, int texHeight, int xBorder, int yBorder) {
        this.identifier = identifier;
        this.width = texWidth;
        this.height = texHeight;
        this.xBorder = xBorder;
        this.yBorder = yBorder;
    }

    /// Constructs a new square 3x3 graphic. Equivalent to calling {@link #ThreeByThree(Identifier, int, int, int, int)}
    /// with texWidth and texHeight the same value as well as xBorder and yBorder the same value.
    ///
    /// @param identifier The {@link Identifier} for the texture to use when rendering.
    /// @param texSize The expected size of the texture
    /// @param border The size of the corners
    public ThreeByThree(@NotNull Identifier identifier, int texSize, int border) {
        this(identifier, texSize, texSize, border, border);
    }

    @Override
    public void draw(@NotNull DrawContext context) {
        var ctxWidth = context.width();
        var ctxHeight = context.height();

        // Upper left
        context.drawImage(
            identifier,
            0, 0,
            xBorder, yBorder,
            0, 0,
            width, height
        );
        // Upper middle
        context.drawImage(
            identifier,
            xBorder, 0,
            ctxWidth - xBorder * 2, yBorder,
            xBorder, 0,
            1, yBorder,
            width, height
        );
        // Upper right
        context.drawImage(
            identifier,
            ctxWidth - xBorder, 0,
            xBorder, yBorder,
            width - xBorder, 0,
            width, height
        );
        // Middle left
        context.drawImage(
            identifier,
            0, yBorder,
            xBorder, ctxHeight - yBorder * 2,
            0, yBorder,
            xBorder, height - yBorder * 2,
            width, height
        );
        // Middle
        context.drawImage(
            identifier,
            xBorder, yBorder,
            ctxWidth - xBorder * 2, ctxHeight - yBorder * 2,
            xBorder, yBorder,
            width - xBorder * 2, height - yBorder * 2,
            width, height
        );
        // Middle right
        context.drawImage(
            identifier,
            ctxWidth - xBorder, yBorder,
            xBorder, ctxHeight - yBorder * 2,
            width - xBorder, yBorder,
            xBorder, height - yBorder * 2,
            width, height
        );
        // Bottom left
        context.drawImage(
            identifier,
            0, ctxHeight - yBorder,
            xBorder, yBorder,
            0, height - yBorder,
            width, height
        );
        // Bottom middle
        context.drawImage(
            identifier,
            xBorder, ctxHeight - yBorder,
            ctxWidth - xBorder * 2, yBorder,
            xBorder, height - yBorder,
            1, yBorder,
            width, height
        );
        // Bottom right
        context.drawImage(
            identifier,
            ctxWidth - xBorder, ctxHeight - yBorder,
            xBorder, yBorder,
            width - xBorder, height - yBorder,
            width, height
        );
    }
}

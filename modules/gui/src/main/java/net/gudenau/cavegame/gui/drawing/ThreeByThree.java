package net.gudenau.cavegame.gui.drawing;

import net.gudenau.cavegame.resource.Identifier;
import org.jetbrains.annotations.NotNull;

public final class ThreeByThree implements Drawable {
    @NotNull
    private final Identifier identifier;
    private final int width;
    private final int height;
    private final int xBorder;
    private final int yBorder;

    public ThreeByThree(@NotNull Identifier identifier, int texWidth, int texHeight, int xBorder, int yBorder) {
        this.identifier = identifier;
        this.width = texWidth;
        this.height = texHeight;
        this.xBorder = xBorder;
        this.yBorder = yBorder;
    }

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

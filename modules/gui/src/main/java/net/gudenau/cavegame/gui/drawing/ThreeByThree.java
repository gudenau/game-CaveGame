package net.gudenau.cavegame.gui.drawing;

import net.gudenau.cavegame.resource.Identifier;
import org.jetbrains.annotations.NotNull;

public final class ThreeByThree implements Drawable {
    @NotNull
    private Identifier identifier;
    private final int size;
    private final int border;

    public ThreeByThree(@NotNull Identifier identifier, int size, int border) {
        this.identifier = identifier;
        this.size = size;
        this.border = border;
    }

    @Override
    public void draw(@NotNull DrawContext context) {
        var width = context.width();
        var height = context.height();

        context.drawRectangle(0, 0, width, height, 0xFF000000);

        // Upper left
        context.drawImage(identifier, 0, 0, border, border, 0, 0, size, size);
        // Upper middle
        context.drawImage(identifier, border, 0, width - border * 2, border, border, 0, 1, border, size, size);
        // Upper right
        context.drawImage(identifier, width - border, 0, border, border, size - border, 0, size, size);
        // Middle left
        context.drawImage(identifier, 0, border, border, height - border * 2, 0, border, border, 1, size, size);
        // Middle
        context.drawImage(identifier, border, border, width - border * 2, height - border * 2, border, border, 1, 1, size, size);
        // Middle right
        context.drawImage(identifier, width - border, border, border, height - border * 2, size - border, border, border, 1, size, size);
        // Bottom left
        context.drawImage(identifier, 0, height - border, border, border, 0, size - border, size, size);
        // Bottom middle
        context.drawImage(identifier, border, height - border, width - border * 2, border, border, size - border, 1, border, size, size);
        // Bottom right
        context.drawImage(identifier, width - border, height - border, border, border, size - border, size - border, size, size);
    }

    public int border() {
        return border;
    }

    public int size() {
        return size;
    }
}

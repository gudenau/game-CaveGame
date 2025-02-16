package net.gudenau.cavegame.gui.drawing;

import net.gudenau.cavegame.resource.Identifier;
import org.jetbrains.annotations.NotNull;

public interface DrawContext {
    interface StackEntry extends AutoCloseable {
        @Override void close();
    }

    int width();
    int height();

    @NotNull StackEntry scissor(int x, int y, int width, int height);

    void drawRectangle(int x, int y, int width, int height, int color);
    void drawText(int x, int y, @NotNull String text, int color);

    void drawImage(@NotNull Identifier identifier, int x, int y, int width, int height, int u, int v, int uWidth, int uHeight, int textureWidth, int textureHeight);

    default void drawImage(@NotNull Identifier identifier, int x, int y, int width, int height, int u, int v, int textureWidth, int textureHeight) {
        drawImage(identifier, x, y, width, height, u, v, width, height, textureWidth, textureHeight);
    }
}

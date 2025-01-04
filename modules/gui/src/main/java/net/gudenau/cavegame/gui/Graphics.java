package net.gudenau.cavegame.gui;

import org.jetbrains.annotations.NotNull;

public interface Graphics {
    interface StackEntry extends AutoCloseable {
        @Override void close();
    }

    @NotNull StackEntry scissor(int x, int y, int width, int height);

    void drawRectangle(int x, int y, int width, int height, int color);
    void drawText(int x, int y, @NotNull String text, int color);
}

package net.gudenau.cavegame.renderer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntConsumer;

public interface Window extends AutoCloseable {
    void visible(boolean visible);
    boolean visible();

    boolean closeRequested();

    void bind();
    void release();

    void flip();

    @Override void close();

    void position(int x, int y);

    @NotNull Size size();

    void resizeCallback(@Nullable ResizeCallback callback);

    record Size(int width, int height) {}

    @FunctionalInterface
    interface ResizeCallback {
        void invoke(Window window, int width, int height);
    }
}

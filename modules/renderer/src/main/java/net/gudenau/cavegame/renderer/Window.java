package net.gudenau.cavegame.renderer;

import org.jetbrains.annotations.NotNull;

//TODO Make a GlfwWindow type to reduce code reuse
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

    record Size(int width, int height) {}
}

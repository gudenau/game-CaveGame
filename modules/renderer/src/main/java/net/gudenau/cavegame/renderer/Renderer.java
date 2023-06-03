package net.gudenau.cavegame.renderer;

public interface Renderer extends AutoCloseable {
    @Override void close();

    void draw();
}

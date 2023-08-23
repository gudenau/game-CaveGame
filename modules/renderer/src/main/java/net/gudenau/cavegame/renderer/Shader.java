package net.gudenau.cavegame.renderer;

public interface Shader extends AutoCloseable {
    @Override void close();
}

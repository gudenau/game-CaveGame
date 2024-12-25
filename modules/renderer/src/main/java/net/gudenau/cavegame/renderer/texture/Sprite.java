package net.gudenau.cavegame.renderer.texture;

public interface Sprite extends Texture {
    @Override
    default void close() {}
}

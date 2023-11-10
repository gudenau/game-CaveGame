package net.gudenau.cavegame.renderer.texture;

public enum TextureFormat {
    RGBA(4),
    RGB(3),
    GRAYSCALE(1),
    ;

    private final int size;

    TextureFormat(int size) {
        this.size = size;
    }

    public int size() {
        return size;
    }
}

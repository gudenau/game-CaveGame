package net.gudenau.cavegame.renderer.shader;

public enum AttributeType {
    FLOAT(Float.BYTES),
    ;

    private final int size;

    AttributeType(int size) {
        this.size = size;
    }

    public int size() {
        return size;
    }
}

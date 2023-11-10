package net.gudenau.cavegame.renderer.shader;

public enum AttributeType {
    FLOAT(Float.BYTES),
    STRUCT(0),
    SAMPLER(0),
    ;

    private final int size;

    AttributeType(int size) {
        this.size = size;
    }

    public int size() {
        return size;
    }
}

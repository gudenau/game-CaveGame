package net.gudenau.cavegame.renderer.shader;


import net.gudenau.codec.Codec;

public enum AttributeUsage {
    POSITION,
    COLOR,
    TEXTURE_COORD,
    ;

    public static final Codec<AttributeUsage> CODEC = Codec.enumeration(AttributeUsage.class);
}

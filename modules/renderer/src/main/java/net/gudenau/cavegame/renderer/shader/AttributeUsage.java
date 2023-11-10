package net.gudenau.cavegame.renderer.shader;

import net.gudenau.cavegame.codec.Codec;
import net.gudenau.cavegame.codec.CodecBuilder;

public enum AttributeUsage {
    POSITION,
    COLOR,
    TEXTURE_COORD,
    ;

    public static final Codec<AttributeUsage> CODEC = CodecBuilder.ofEnum(AttributeUsage.class);
}

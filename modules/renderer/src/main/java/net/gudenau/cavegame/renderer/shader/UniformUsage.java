package net.gudenau.cavegame.renderer.shader;

import net.gudenau.cavegame.codec.Codec;
import net.gudenau.cavegame.codec.CodecBuilder;

public enum UniformUsage {
    MODEL,
    VIEW,
    PROJECTION,
    ;

    public static final Codec<UniformUsage> CODEC = CodecBuilder.ofEnum(UniformUsage.class);
}

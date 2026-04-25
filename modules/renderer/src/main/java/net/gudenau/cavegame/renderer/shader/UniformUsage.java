package net.gudenau.cavegame.renderer.shader;

import net.gudenau.codec.Codec;

public enum UniformUsage {
    UBO,
    ;

    public static final Codec<UniformUsage> CODEC = Codec.enumeration(UniformUsage.class);
}

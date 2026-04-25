package net.gudenau.cavegame.renderer.shader;

import net.gudenau.codec.Codec;
import org.jetbrains.annotations.NotNull;

public enum ShaderType {
    FRAGMENT("frag"),
    VERTEX("vert"),
    ;

    public static final Codec<ShaderType> CODEC = Codec.enumeration(ShaderType.class);

    @NotNull
    private final String extension;

    ShaderType(@NotNull String extension) {
        this.extension = extension;
    }

    @NotNull
    public String extension() {
        return extension;
    }
}

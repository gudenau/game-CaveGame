package net.gudenau.cavegame.renderer.shader;

import net.gudenau.cavegame.codec.Codec;
import net.gudenau.cavegame.codec.CodecBuilder;
import org.jetbrains.annotations.NotNull;

public enum ShaderType {
    FRAGMENT("frag"),
    VERTEX("vert"),
    ;

    public static final Codec<ShaderType> CODEC = CodecBuilder.ofEnum(ShaderType.class);

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

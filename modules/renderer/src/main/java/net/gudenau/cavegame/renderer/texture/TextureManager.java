package net.gudenau.cavegame.renderer.texture;

import net.gudenau.cavegame.resource.Identifier;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public interface TextureManager {
    @NotNull
    default Texture loadTexture(@NotNull Identifier identifier) throws IOException {
        return loadTexture(identifier, TextureFormat.RGBA);
    }

    @NotNull
    Texture loadTexture(@NotNull Identifier identifier, @NotNull TextureFormat format) throws IOException;

    @NotNull
    default Font loadFont(@NotNull Identifier identifier) throws IOException {
        return loadFont(identifier, TextureFormat.GRAYSCALE);
    }

    @NotNull
    Font loadFont(@NotNull Identifier identifier, @NotNull TextureFormat format) throws IOException;
}

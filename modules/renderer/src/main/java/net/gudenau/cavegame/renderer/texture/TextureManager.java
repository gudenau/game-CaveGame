package net.gudenau.cavegame.renderer.texture;

import net.gudenau.cavegame.resource.Identifier;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Optional;

public interface TextureManager {
    int MISSING_TEXTURE_SIZE = 256;
    int MISSING_TEXTURE_BLACK = 0xFF000000;
    int MISSING_TEXTURE_MAGENTA = 0xFFFF00FF;
    Identifier MISSING_TEXTURE = Identifier.cavegame("missingno");

    @NotNull
    Texture createTexture(@NotNull Identifier identifier, @NotNull NativeTexture texture);

    @NotNull
    default Texture loadTexture(@NotNull Identifier identifier) throws IOException {
        return loadTexture(identifier, TextureFormat.RGBA);
    }

    @NotNull
    Texture loadTexture(@NotNull Identifier identifier, @NotNull TextureFormat format) throws IOException;

    @NotNull
    Optional<Texture> getTexture(@NotNull Identifier identifier);

    @NotNull
    default Texture missingTexture() {
        return getTexture(MISSING_TEXTURE)
            .orElseThrow(() -> new IllegalStateException("missingno was not loaded!"));
    }

    @NotNull
    default SpriteAtlas loadAtlas(@NotNull Identifier identifier) throws IOException {
        return loadAtlas(identifier, TextureFormat.RGBA);
    }

    @NotNull SpriteAtlas loadAtlas(@NotNull Identifier identifier, @NotNull TextureFormat textureFormat) throws IOException;

    @NotNull
    default Font loadFont(@NotNull Identifier identifier) throws IOException {
        return loadFont(identifier, TextureFormat.GRAYSCALE);
    }

    @NotNull
    Font loadFont(@NotNull Identifier identifier, @NotNull TextureFormat format) throws IOException;
}

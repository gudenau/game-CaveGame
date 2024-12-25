package net.gudenau.cavegame.renderer.texture;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.stream.Stream;

public interface AtlasedTexture<K> extends Texture {
    @NotNull Optional<Sprite> sprite(@NotNull K key);

    @NotNull Stream<Sprite> sprites();
}

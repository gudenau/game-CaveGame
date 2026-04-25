package net.gudenau.cavegame.renderer.vk.texture;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.gudenau.cavegame.logger.Logger;
import net.gudenau.cavegame.renderer.BufferType;
import net.gudenau.cavegame.renderer.font.FreeTypeFont;
import net.gudenau.cavegame.renderer.texture.*;
import net.gudenau.cavegame.renderer.vk.VkGraphicsBuffer;
import net.gudenau.cavegame.renderer.vk.VkRenderer;
import net.gudenau.cavegame.resource.Identifier;
import net.gudenau.cavegame.resource.ResourceLoader;
import net.gudenau.cavegame.util.SharedLock;
import net.gudenau.codec.ops.json.JsonOps;
import net.gudenau.codec.util.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Collectors;

public final class VulkanTextureManager implements TextureManager, AutoCloseable {
    private final VkRenderer renderer;
    private final SharedLock samplers$lock = new SharedLock();
    private final Int2ObjectMap<VulkanSampler> samplers = new Int2ObjectOpenHashMap<>();

    private final SharedLock textureLock = new SharedLock();
    private final Map<Identifier, VulkanTexture> textures = new HashMap<>();
    private final Map<Identifier, Font> fonts = new HashMap<>();

    public VulkanTextureManager(VkRenderer renderer) {
        this.renderer = renderer;

        try(var texture = NativeTexture.create(MISSING_TEXTURE_SIZE, MISSING_TEXTURE_SIZE, TextureFormat.RGBA)) {
            var pixels = texture.pixels().asIntBuffer();

            final int half = MISSING_TEXTURE_SIZE / 2;

            for(int y = 0; y < MISSING_TEXTURE_SIZE; y++) {
                boolean halfY = y >= half;

                for(int x = 0; x < MISSING_TEXTURE_SIZE; x++) {
                    boolean halfX = x >= half;
                    int color = (halfY ^ halfX) ? MISSING_TEXTURE_BLACK : MISSING_TEXTURE_MAGENTA;
                    pixels.put(color);
                }
            }

            createTexture(MISSING_TEXTURE, texture);
        }
    }

    public void close() {
        List.copyOf(textures.values()).forEach(VulkanTexture::close);
        if(!textures.isEmpty()) {
            Logger.forClass(VulkanTextureManager.class)
                .warn(
                    "Not all textures got cleaned up: " +
                        textures.keySet()
                            .stream()
                            .map(Identifier::toString)
                            .collect(Collectors.joining(", "))
                );
        }

        samplers.values().forEach(VulkanSampler::close);
    }

    private VulkanTexture doUpload(@NotNull NativeTexture texture) {
        var pixels = texture.pixels();

        try(var stagingBuffer = renderer.createBuffer(BufferType.STAGING, pixels.remaining())) {
            stagingBuffer.upload(pixels);
            return new VulkanTexture(renderer, this, stagingBuffer, texture);
        }
    }

    @Override
    @NotNull
    public Texture createTexture(@NotNull Identifier identifier, @NotNull NativeTexture texture) {
        var uploaded = doUpload(texture);
        if(textureLock.write(() -> textures.putIfAbsent(identifier, uploaded)) != null) {
            uploaded.close();
            throw new IllegalStateException("Texture " + identifier + " already exits");
        }

        return uploaded;
    }

    @NotNull
    private static NativeTexture readNativeTexture(@NotNull Identifier identifier, @NotNull TextureFormat format) throws IOException {
        NativeTexture imageResult;
        var imageFileBuffer = ResourceLoader.buffer(identifier.normalize("texture", ".png"));
        try {
            imageResult = PngReader.read(imageFileBuffer, format);
        } finally {
            MemoryUtil.memFree(imageFileBuffer);
        }
        return imageResult;
    }

    //TODO Validate format argument on already loaded textures
    @NotNull
    @Override
    public Texture loadTexture(@NotNull Identifier identifier, @NotNull TextureFormat format) throws IOException {
        var texture = textureLock.read(() -> textures.get(identifier));
        if(texture != null) {
            return texture;
        }

        NativeTexture imageResult = readNativeTexture(identifier, format);

        VulkanTexture loadedTexture;
        try {
            loadedTexture = doUpload(imageResult);
        } finally {
            imageResult.close();
        }

        var existing = textureLock.write(() -> textures.putIfAbsent(identifier, loadedTexture));
        if(existing != null) {
            loadedTexture.close();
            return existing;
        } else {
            return loadedTexture;
        }
    }

    @Override
    @NotNull
    public SpriteAtlas loadAtlas(@NotNull Identifier identifier, @NotNull TextureFormat textureFormat) throws IOException {
        {
            var texture = textureLock.read(() -> textures.get(identifier));
            if (texture instanceof SpriteAtlas atlas) {
                return atlas;
            }
            if (texture != null) {
                throw new IllegalStateException("Texture " + identifier + " is not an atlas");
            }
        }

        List<AtlasMetadata> metadata;
        switch (ResourceLoader.decodeJson(AtlasMetadata.CODEC_LIST, identifier.normalize("atlas", ".json"))) {
            case Result.Value(var value) -> metadata = value;

            case Result.Error(var error, var _) ->
                throw new IOException("Failed to read metadata " + identifier + ": " + error.get());
        }

        try (var scope = StructuredTaskScope.open()) {
            for (var metadatum : metadata) {
                scope.fork(() -> {
                    NativeTexture texture;
                    try {
                        texture = readNativeTexture(metadatum.texture(), textureFormat);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read texture " + metadatum.texture());
                    }

                    return texture;
                });
            }
        }

        var created = new VulkanSpriteAtlas(renderer, this, textureFormat);

        var existing = textureLock.write(() -> textures.putIfAbsent(identifier, created));
        if (existing != null) {
            created.close();

            if (existing instanceof SpriteAtlas atlas) {
                return atlas;
            }
        }

        return created;
    }

    @Override
    @NotNull
    public Font loadFont(@NotNull Identifier identifier, @NotNull TextureFormat format) throws IOException {
        {
            var texture = textureLock.read(() -> textures.get(identifier));
            if(texture instanceof Font font) {
                return font;
            }
        }

        final class GlyphTexture implements AutoCloseable {
            private final int character;
            @Nullable
            private final NativeTexture texture;

            private int x = -1;
            private int y = -1;

            private GlyphTexture(@NotNull FreeTypeFont.Glyph glyph) {
                character = glyph.character();

                if(glyph.pixels() != null) {
                    texture = NativeTexture.copyOf(glyph.width(), glyph.height(), glyph.format(), glyph.pixels());
                } else {
                    texture = null;
                }
            }

            private boolean hasTexture() {
                return texture != null;
            }

            @Override
            public void close() {
                if(texture != null) {
                    texture.close();
                }
            }
        }

        TexturePacker<GlyphTexture> packer = new TexturePacker<>(1);
        try {
            var glyphs = new ArrayList<GlyphTexture>();

            var fontBuffer = ResourceLoader.buffer(identifier.normalize("font", ".ttf"));
            try {
                try(var font = FreeTypeFont.of(fontBuffer)) {
                    try(var stream = font.glyphs()) {
                        stream.map(GlyphTexture::new)
                            .peek(glyphs::add)
                            .filter(GlyphTexture::hasTexture)
                            .forEach((glyph) -> packer.add(glyph.texture.width(), glyph.texture.height(), glyph));
                    }
                }
            } finally {
                MemoryUtil.memFree(fontBuffer);
            }
            packer.pack();

            packer.entries().forEach((entry) -> {
                @SuppressWarnings("resource")
                var glyph = entry.data();
                glyph.x = entry.x();
                glyph.y = entry.y();
            });

            try(var atlasTexture = NativeTexture.create(packer.width(), packer.height(), format)) {
                //noinspection DataFlowIssue
                glyphs.stream()
                    .filter(GlyphTexture::hasTexture)
                    .forEach((glyph) -> atlasTexture.blit(glyph.x, glyph.y, glyph.texture));

                VkGraphicsBuffer stagingBuffer = renderer.createBuffer(BufferType.STAGING, atlasTexture.pixels().remaining());
                stagingBuffer.upload(atlasTexture.pixels());

                VulkanFont texture;
                try {
                    texture = new VulkanFont(
                        renderer,
                        this,
                        stagingBuffer,
                        atlasTexture
                    );
                } finally {
                    stagingBuffer.close();
                }

                var existing = textureLock.write(() -> textures.putIfAbsent(identifier, texture));
                if(existing != null) {
                    if(!(existing instanceof Font font)) {
                        throw new IllegalStateException("Texture " + identifier + " was loaded as a plain texture, not a font");
                    }

                    texture.close();
                    return font;
                } else {
                    return texture;
                }
            }
        } finally {
            try {
                packer.entries()
                    .map(TexturePacker.Entry::data)
                    .forEach(GlyphTexture::close);
                // If something blows up before the pack call, prevent the ISE from taking over
            } catch(IllegalStateException _) {}
        }
    }

    @NotNull
    public Optional<Texture> getTexture(@NotNull Identifier identifier) {
        return Optional.ofNullable(textures.get(identifier));
    }

    public VulkanSampler sampler(int mipLevels) {
        var sampler = samplers$lock.read(() -> samplers.get(mipLevels));
        if(sampler != null) {
            return sampler;
        }
        return samplers$lock.write(() -> samplers.computeIfAbsent(mipLevels, (level) -> new VulkanSampler(renderer.logicalDevice(), mipLevels)));
    }

    public void removeTexture(@NotNull VulkanTexture texture) {
        var keys = textureLock.read(() -> textures.entrySet()
            .stream()
            .filter((e) -> e.getValue() == texture)
            .map(Map.Entry::getKey)
            .collect(Collectors.toUnmodifiableSet())
        );

        textureLock.write(() ->
            keys.forEach((key) -> textures.remove(key, texture))
        );
    }
}

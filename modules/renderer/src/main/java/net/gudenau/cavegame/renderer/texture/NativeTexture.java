package net.gudenau.cavegame.renderer.texture;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Objects;

public final class NativeTexture implements Texture {
    @NotNull
    public static NativeTexture read(@NotNull ByteBuffer buffer) throws IOException {
        return read(buffer, TextureFormat.RGBA);
    }

    @NotNull
    public static NativeTexture read(@NotNull ByteBuffer buffer, @NotNull TextureFormat format) throws IOException {
        return PngReader.read(buffer, format);
    }

    @NotNull
    public static NativeTexture create(int width, int height, @NotNull TextureFormat format) {
        var pixels = MemoryUtil.memCalloc(width * height * format.size());
        return new NativeTexture(width, height, format, pixels, () -> MemoryUtil.memFree(pixels));
    }

    @NotNull
    public static NativeTexture copyOf(int width, int height, @NotNull TextureFormat format, @NotNull ByteBuffer pixels) {
        var copy = MemoryUtil.memAlloc(pixels.capacity());
        MemoryUtil.memCopy(pixels, copy);
        return new NativeTexture(width, height, format, copy, () -> MemoryUtil.memFree(copy));
    }

    @NotNull
    public static NativeTexture of(int width, int height, @NotNull TextureFormat format, @NotNull ByteBuffer pixels, @NotNull Runnable cleaner) {
        Objects.requireNonNull(format);
        Objects.requireNonNull(pixels);
        Objects.requireNonNull(cleaner);

        return new NativeTexture(width, height, format, pixels, cleaner);
    }

    private final int width;
    private final int height;
    private final TextureFormat format;
    private final ByteBuffer pixels;
    private final Runnable cleaner;

    private NativeTexture(int width, int height, @NotNull TextureFormat format, @NotNull ByteBuffer pixels, @NotNull Runnable cleaner) {
        this.width = width;
        this.height = height;
        this.format = format;
        this.pixels = pixels;
        this.cleaner = cleaner;
    }

    //TODO Replace this with something that isn't AWT (Apple breaks it)/STB (maintainer hates security)
    public void save(@NotNull Path destination) throws IOException {
        throw new UnsupportedOperationException("Implement image saving with SPNG");
    }

    public void blit(int x, int y, @NotNull NativeTexture texture) {
        if(texture.format() != format()) {
            throw new UnsupportedOperationException("Blitting between different formats is not yet implemented");
        }
        if(x < 0 || y < 0 || x + texture.width() >= width() || y + texture.height() >= height()) {
            throw new IllegalArgumentException("Texture is out of bounds");
        }

        var srcPixels = MemorySegment.ofBuffer(texture.pixels());
        var srcPitch = texture.width() * texture.format().size();
        var srcOffset = 0;

        var dstPixels = MemorySegment.ofBuffer(pixels);
        var dstPitch = width * format.size();
        var dstOffset = (x + y * width) * format.size();

        for(int i = 0; i < texture.height(); i++) {
            MemorySegment.copy(srcPixels, srcOffset, dstPixels, dstOffset, srcPitch);
            srcOffset += srcPitch;
            dstOffset += dstPitch;
        }
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    @NotNull
    public TextureFormat format() {
        return format;
    }

    @NotNull
    public ByteBuffer pixels() {
        return pixels;
    }

    @Override
    public void close() {
        cleaner.run();
    }
}

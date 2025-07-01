package net.gudenau.cavegame.renderer.texture;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.spng.spng_ihdr;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.spng.SPNG.*;

public final class PngReader {
    private PngReader() {
        throw new AssertionError();
    }

    @NotNull
    public static NativeTexture read(@NotNull ByteBuffer fileBuffer, @NotNull TextureFormat format) throws IOException {
        var context = spng_ctx_new(0);
        try(var stack = MemoryStack.stackPush()) {
            spng_set_png_buffer(context, fileBuffer);

            var header = spng_ihdr.calloc(stack);
            spng_get_ihdr(context, header);

            var width = header.width();
            var height = header.height();

            var spngFormat = switch(format) {
                case RGBA -> SPNG_FMT_RGBA8;
                case RGB -> SPNG_FMT_RGB8;
                case GRAYSCALE -> SPNG_FMT_G8;
            };

            var sizePointer = stack.pointers(NULL);
            spng_decoded_image_size(context, spngFormat, sizePointer);
            var size = sizePointer.get(0);

            //TODO Find a better limit for the max image size
            if(size > Integer.MAX_VALUE) {
                throw new IOException("Decoded image would be too large!");
            }

            var pixels = MemoryUtil.memAlloc((int) size);
            spng_decode_image(context, pixels, spngFormat, 0);

            return NativeTexture.of(width, height, format, pixels, () -> MemoryUtil.memFree(pixels));
        } finally {
            spng_ctx_free(context);
        }
    }
}

package net.gudenau.cavegame.renderer.texture;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.spng.SPNG;
import org.lwjgl.util.spng.spng_ihdr;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.spng.SPNG.*;

public final class PngWriter {
    private PngWriter() {
        throw new AssertionError();
    }

    public static void write(@NotNull Path destination, @NotNull ByteBuffer pixels, int width, int height, @NotNull TextureFormat format) throws IOException {
        var png = write(pixels, width, height, format);
        try(var channel = Files.newByteChannel(destination, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            while(png.hasRemaining()) {
                channel.write(png);
            }
        } catch(IOException e) {
            throw new IOException("Failed to write PNG to disk", e);
        } finally {
            MemoryUtil.memFree(png);
        }
    }

    @NotNull
    public static ByteBuffer write(@NotNull ByteBuffer pixels, int width, int height, @NotNull TextureFormat format) {
        var context = spng_ctx_new(SPNG_CTX_ENCODER);
        if(context == NULL) {
            throw new RuntimeException("Failed to create SPNG context");
        }

        try(var stack = MemoryStack.stackPush()) {
            var result = spng_set_option(context, SPNG_ENCODE_TO_BUFFER, 1);
            if(result != SPNG_OK) {
                throw new RuntimeException("Failed to set SPNG options: " + spng_strerror(result));
            }

            var header = spng_ihdr.calloc(stack)
                .width(width)
                .height(height)
                .color_type(switch(format) {
                    case RGBA -> SPNG_COLOR_TYPE_TRUECOLOR_ALPHA;
                    case RGB -> SPNG_COLOR_TYPE_TRUECOLOR;
                    case GRAYSCALE -> SPNG_COLOR_TYPE_GRAYSCALE;
                })
                .bit_depth((byte) 8);

            result = spng_set_ihdr(context, header);
            if(result != SPNG_OK) {
                throw new RuntimeException("Failed to set SPNG IHDR: " + spng_strerror(result));
            }

            result = spng_encode_image(context, pixels, SPNG_FMT_PNG, SPNG_ENCODE_FINALIZE);
            if(result != SPNG_OK) {
                throw new RuntimeException("Failed to encode PNG: " + spng_strerror(result));
            }

            var resultPointer = stack.ints(0);
            var png = spng_get_png_buffer(context, resultPointer);
            result = resultPointer.get(0);
            if(result != SPNG_OK) {
                throw new RuntimeException("Failed to get PNG buffer: " + spng_strerror(result));
            }

            return png;
        } finally {
            spng_ctx_free(context);
        }
    }
}

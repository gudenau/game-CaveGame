package net.gudenau.cavegame.renderer.font;

import net.gudenau.cavegame.renderer.texture.TextureFormat;
import net.gudenau.cavegame.util.collection.ThreadQueue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Face;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.freetype.FreeType.*;

public final class FreeTypeFont implements AutoCloseable {
    @NotNull
    public static FreeTypeFont of(@NotNull ByteBuffer buffer) {
        return new FreeTypeFont(buffer);
    }

    public record Glyph(
        int character,
        int glyph,
        @Nullable TextureFormat format,
        int xOff, int yOff,
        int width, int height, int pitch,
        @Nullable ByteBuffer pixels
    ) {}

    private final Lock lock = new ReentrantLock();
    private final ByteBuffer fontBuffer;
    private final FT_Face handle;

    private FreeTypeFont(@NotNull ByteBuffer buffer) {
        this.fontBuffer = buffer;

        try(var freeType = LibInstance.get()) {
            handle = freeType.createFace(fontBuffer);
        }

        checked(
            FT_Set_Char_Size(handle, 0, 16 * 64, 300, 300),
            "Failed to set FreeType font size"
        );
    }

    @NotNull
    public Stream<Glyph> glyphs() {
        lock.lock();

        var state = new Object() {
            IntBuffer glyphId = MemoryUtil.memCallocInt(1);
            ByteBuffer lastPixels = null;

            void free() {
                MemoryUtil.memFree(glyphId);
                MemoryUtil.memFree(lastPixels);
            }
        };
        Iterable<Glyph> iterator = () -> new Iterator<>() {
            long character = FT_Get_First_Char(handle, state.glyphId);
            {
                while((character & 0xFFFFFFFF00000000L) != 0) {
                    character = FT_Get_Next_Char(handle, character, state.glyphId);
                }
            }

            @Override
            public boolean hasNext() {
                return character != 0;
            }

            @Override
            public Glyph next() {
                var glyph = handle.glyph();

                TextureFormat format = null;
                int xOff = 0;
                int yOff = 0;
                int width = -1;
                int height = -1;
                int pitch = -1;
                ByteBuffer pixels = null;

                if(FT_Render_Glyph(glyph, FT_RENDER_MODE_NORMAL) == FT_Err_Ok) {
                    var bitmap = glyph.bitmap();

                    xOff = glyph.bitmap_left() * 64;
                    yOff = -glyph.bitmap_top() * 64;
                    width = bitmap.width();
                    height = bitmap.rows();
                    pitch = bitmap.pitch();

                    if(width != 0 && height != 0 && pitch != 0) {
                        if(glyph.format() != FT_GLYPH_FORMAT_BITMAP) {
                            throw new RuntimeException("Don't know how to handle glyphs with format " + switch(glyph.format()) {
                                case /*FT_GLYPH_FORMAT_NONE*/ 0x00000000 -> "none";
                                case /*FT_GLYPH_FORMAT_COMPOSITE*/ 0x636F6D70 -> "composite";
                                case /*FT_GLYPH_FORMAT_OUTLINE*/ 0x6F75746C -> "outline";
                                case /*FT_GLYPH_FORMAT_PLOTTER*/ 0x706C6F74 -> "plotter";
                                case /*FT_GLYPH_FORMAT_SVG*/ 0x53564720 -> "svg";

                                default -> "0x%08X".formatted(glyph.format());
                            });
                        }
                        format = switch(bitmap.pixel_mode()) {
                            case FT_PIXEL_MODE_GRAY -> TextureFormat.GRAYSCALE;
                            default -> throw new RuntimeException("Don't know how to handle glyphs with pixel mode " + switch(bitmap.pixel_mode()) {
                                case FT_PIXEL_MODE_NONE -> "NONE";
                                case FT_PIXEL_MODE_MONO -> "MONO";
                                case FT_PIXEL_MODE_GRAY2 -> "GRAY2";
                                case FT_PIXEL_MODE_GRAY4 -> "GRAY4";
                                case FT_PIXEL_MODE_LCD -> "LCD";
                                case FT_PIXEL_MODE_LCD_V -> "LCD_V";
                                case FT_PIXEL_MODE_BGRA -> "BGRA";
                                case FT_PIXEL_MODE_MAX -> "MAX";
                                default -> "0x%08X".formatted(bitmap.pixel_mode());
                            });
                        };

                        var temp = bitmap.buffer(height * pitch);
                        var lastPixels = state.lastPixels;
                        if(lastPixels == null || temp.capacity() > lastPixels.capacity()) {
                            state.lastPixels = lastPixels = MemoryUtil.memRealloc(lastPixels, temp.capacity());
                        }
                        lastPixels.limit(temp.capacity());
                        MemoryUtil.memCopy(temp, lastPixels);
                        pixels = lastPixels;
                    }
                }

                var result = new Glyph(
                    (int) character, state.glyphId.get(0),
                    format,
                    xOff, yOff,
                    width, height, pitch,
                    pixels
                );

                checked(
                    FT_Load_Glyph(handle, state.glyphId.get(0), FT_LOAD_DEFAULT /* FT_LOAD_COMPUTE_METRICS */),
                    "Failed to load glyph " + Character.toString((int) character)
                );

                do {
                    character = FT_Get_Next_Char(handle, character, state.glyphId);
                } while((character & 0xFFFFFFFF00000000L) != 0);

                return result;
            }
        };
        return StreamSupport.stream(iterator.spliterator(), false)
            .onClose(() -> {
                state.free();
                lock.unlock();
            });
    }

    @Override
    public void close() {
        try(var freeType = LibInstance.get()) {
            freeType.destroyFace(handle);
        }
    }

    private static void checked(int result, @NotNull String message) {
        if(result != FT_Err_Ok) {
            var error = FT_Error_String(result);
            throw new RuntimeException(message + ": " + (error == null ? String.valueOf(result) : error));
        }
    }

    private static final class LibInstance implements ThreadQueue.Cleaner, AutoCloseable {
        private static final ThreadQueue<LibInstance> INSTANCES = ThreadQueue.of(LibInstance::new);

        @NotNull
        private static LibInstance get() {
            return INSTANCES.get();
        }

        private final long handle;

        private LibInstance() {
            try(var stack = MemoryStack.stackPush()) {
                var handlePointer = stack.pointers(NULL);
                checked(FT_Init_FreeType(handlePointer), "Failed to init FreeType");
                handle = handlePointer.get(0);
            }
        }

        @NotNull
        private FT_Face createFace(@NotNull ByteBuffer fontBuffer) {
            try(var stack = MemoryStack.stackPush()) {
                var pointer = stack.pointers(NULL);
                checked(FT_New_Memory_Face(handle, fontBuffer, 0, pointer), "Failed to create FreeType face");
                return FT_Face.create(pointer.get(0));
            }
        }

        private void destroyFace(@NotNull FT_Face face) {
            checked(FT_Done_Face(face), "Failed to destroy FreeType face");
        }

        @Override
        public void cleanup() {
            FT_Done_FreeType(handle);
        }

        @Override
        public void close() {
            INSTANCES.put(this);
        }
    }
}

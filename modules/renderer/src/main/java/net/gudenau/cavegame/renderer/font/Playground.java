// Uses AWT, unused but I don't want to delete it until font rendering is properly finalized.
/*
package net.gudenau.cavegame.renderer.font;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.IntFunction;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.freetype.FreeType.*;
import static org.lwjgl.util.freetype.FreeType.FT_Set_Char_Size;
import static org.lwjgl.util.harfbuzz.HarfBuzz.*;

public class Playground {
    private static final Path TEST_FONT = Path.of("/usr/share/fonts/TTF/DejaVuSans.ttf");

    public static void main(String[] args) throws Throwable {
        Configuration.HARFBUZZ_LIBRARY_NAME.set(FreeType.getLibrary());

        ByteBuffer fontBuffer;
        try(var channel = Files.newByteChannel(TEST_FONT, StandardOpenOption.READ)) {
            fontBuffer = MemoryUtil.memAlloc((int) channel.size());
            while(fontBuffer.hasRemaining()) {
                channel.read(fontBuffer);
            }
            fontBuffer.flip();
        }

        try {
            long freeType;
            try(var stack = MemoryStack.stackPush()) {
                var pointer = stack.pointers(NULL);
                ftChecked(FT_Init_FreeType(pointer));
                freeType = pointer.get(0);
            }
            try {
                FT_Face freeTypeFace;
                try(var stack = MemoryStack.stackPush()) {
                    var pointer = stack.pointers(NULL);
                    ftChecked(FT_New_Memory_Face(freeType, fontBuffer, 0, pointer));
                    freeTypeFace = FT_Face.create(pointer.get(0));
                }
                try {
                    ftChecked(FT_Set_Char_Size(freeTypeFace, 0, 16 * 64, 300, 300));

                    /* TODO Replace the n call for this when the bindings are corrected
                    var harfBuzzFont = hb_ft_font_create(freeTypeFace.address(), null);
                     * /
                    var harfBuzzFont = nhb_ft_font_create(freeTypeFace.address(), NULL);

                    try {
                        hb_ft_font_set_funcs(harfBuzzFont);

                        var string = """
                            Hello world!
                            But with lines!
                            """;

                        var canvas = new BufferedImage(512, 256, BufferedImage.TYPE_4BYTE_ABGR);
                        var canvasGraphics = canvas.createGraphics();
                        try {
                            var state = new Object() {
                                int yOffset = 0;
                            };

                            string.trim().lines().forEachOrdered((line) -> {
                                var harfBuzzBuffer = hb_buffer_create();
                                try {
                                    hb_buffer_add_utf8(harfBuzzBuffer, line, 0, -1);

                                    hb_buffer_set_direction(harfBuzzBuffer, HB_DIRECTION_LTR);
                                    hb_buffer_set_script(harfBuzzBuffer, HB_SCRIPT_LATIN);
                                    hb_buffer_set_language(harfBuzzBuffer, hb_language_from_string("en"));

                                    hb_shape(harfBuzzFont, harfBuzzBuffer, null);

                                    @SuppressWarnings("resource")
                                    var glyphInfos = hb_buffer_get_glyph_infos(harfBuzzBuffer);
                                    @SuppressWarnings("resource")
                                    var glyphPositions = hb_buffer_get_glyph_positions(harfBuzzBuffer);
                                    var count = Math.min(glyphInfos.capacity(), glyphPositions.capacity());

                                    int cursorX = 0;

                                    state.yOffset += freeTypeFace.size().metrics().height();
                                    int cursorY = state.yOffset;

                                    record Glyph(
                                        int xOffset,
                                        int yOffset,
                                        @Nullable BufferedImage image
                                    ) {}

                                    var glyphImages = new Int2ObjectOpenHashMap<Glyph>();

                                    for(int i = 0; i < count; i++) {
                                        var glyphInfo = glyphInfos.get(i);
                                        var glyphId = glyphInfo.codepoint();

                                        var glyphData = glyphImages.computeIfAbsent(glyphId, (IntFunction<Glyph>) (index) -> {
                                            ftChecked(FT_Load_Glyph(freeTypeFace, index, FT_LOAD_COMPUTE_METRICS));
                                            //ftChecked(FT_Load_Char(freeTypeFace, index, FT_LOAD_DEFAULT));
                                            var glyph = freeTypeFace.glyph();
                                            ftChecked(FT_Render_Glyph(glyph, FT_RENDER_MODE_NORMAL));
                                            var bitmap = glyph.bitmap();

                                            var xOffset = glyph.bitmap_left() * 64;
                                            var yOffset = -glyph.bitmap_top() * 64;

                                            var width = bitmap.width();
                                            var height = bitmap.rows();

                                            BufferedImage image = null;
                                            if(width != 0 && height != 0) {
                                                var pitch = bitmap.pitch();
                                                image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
                                                var pixels = new int[width];
                                                var buffer = bitmap.buffer(height * pitch);

                                                for(int y = 0; y < height; y++) {
                                                    int bufferOffset = y * pitch;
                                                    for(int x = 0; x < width; x++) {
                                                        var pixel = Byte.toUnsignedInt(buffer.get(x + bufferOffset));
                                                        pixel <<= 24;
                                                        pixel |= 0x00_FF_FF_FF;
                                                        pixels[x] = pixel;
                                                    }
                                                    image.setRGB(
                                                        0, y,
                                                        width, 1,
                                                        pixels, 0,
                                                        width
                                                    );
                                                }
                                            }

                                            return new Glyph(xOffset, yOffset, image);
                                            //return image;
                                        });

                                        var glyphPosition = glyphPositions.get(i);
                                        var xAdvance = glyphPosition.x_advance();
                                        var yAdvance = glyphPosition.y_advance();
                                        var xOffset = glyphData.xOffset();
                                        var yOffset = glyphData.yOffset();

                                        var glyphImage = glyphData.image();
                                        if(glyphImage != null) {
                                            canvasGraphics.drawImage(
                                                glyphImage,
                                                (cursorX + xOffset) / 64,
                                                (cursorY + yOffset) / 64,
                                                null
                                            );
                                        }

                                        cursorX += xAdvance;
                                        cursorY += yAdvance;
                                    }

                                } finally {
                                    hb_buffer_destroy(harfBuzzBuffer);
                                }
                            });

                            try {
                                Files.deleteIfExists(Path.of("harf.png"));
                                try(var stream = Files.newOutputStream(Path.of("harf.png"), StandardOpenOption.CREATE)) {
                                    ImageIO.write(canvas, "PNG", stream);
                                }
                            } catch(IOException _) {}
                        } finally {
                            canvasGraphics.dispose();
                        }
                    } finally {
                        hb_font_destroy(harfBuzzFont);
                    }
                } finally {
                    FT_Done_Face(freeTypeFace);
                }
            } finally {
                FT_Done_Library(freeType);
            }
        } finally {
            MemoryUtil.memFree(fontBuffer);
        }
    }

    private static void ftChecked(int result) {
        if(result != FT_Err_Ok) {
            throw new RuntimeException(String.valueOf(FT_Error_String(result)));
        }
    }
}
*/

package net.gudenau.cavegame.renderer.texture;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public final class PngReader {
    private PngReader() {
        throw new AssertionError();
    }

    public record Result(
        int width,
        int height,
        @NotNull TextureFormat format,
        @NotNull ByteBuffer pixels
    ) implements AutoCloseable {
        @Override
        public void close() {
            MemoryUtil.memFree(pixels);
        }
    }

    @NotNull
    public static Result read(@NotNull ByteBuffer fileBuffer, @NotNull TextureFormat format) throws IOException {
        //FIXME Find a replacement to AWT, STB is unsafe.

        // Read the image from the buffer via AWT
        var stream = new InputStream() {
            private final ByteBuffer buffer = fileBuffer.slice();

            @Override
            public int read() {
                return buffer.hasRemaining() ? buffer.get() & 0xFF : -1;
            }

            @Override
            public int read(byte @NotNull [] b, int off, int len) {
                int size = Math.min(len, buffer.remaining());
                buffer.get(b, off, size);
                return size;
            }
        };

        var rawImage = ImageIO.read(stream);

        var desiredType = switch(format) {
            case RGBA -> BufferedImage.TYPE_4BYTE_ABGR;
            case RGB -> BufferedImage.TYPE_3BYTE_BGR;
            case GRAYSCALE -> BufferedImage.TYPE_BYTE_GRAY;
        };

        var width = rawImage.getWidth();
        var height = rawImage.getHeight();

        // Convert the AWT image, if required
        BufferedImage convertedImage;
        if(rawImage.getType() != desiredType) {
            convertedImage = new BufferedImage(width, height, desiredType);
            var graphics = convertedImage.createGraphics();
            try {
                graphics.drawImage(rawImage, 0, 0, width, height, null);
            } finally {
                graphics.dispose();
            }
        } else {
            convertedImage = rawImage;
        }

        // Copy the data out of the AWT image
        var size = width * height * format.size();
        var result = MemoryUtil.memAlloc(size);
        var dataBuffer = convertedImage.getRaster().getDataBuffer();

        switch(dataBuffer) {
            case DataBufferByte byteBuffer -> {
                result.put(0, byteBuffer.getData());

                // Swap bytes around
                switch(format) {
                    case RGBA -> {
                        for(int i = 0; i < size; i += 4) {
                            result.putInt(i, Integer.reverseBytes(result.getInt(i)));
                        }
                    }

                    case RGB -> {
                        for(int i = 0; i < size; i += 3) {
                            byte tmp = result.get(i);
                            result.put(i, result.get(i + 2));
                            result.put(i + 2, tmp);
                        }
                    }
                }
            }
            case DataBufferInt intBuffer -> result.asIntBuffer().put(0, intBuffer.getData());
            default -> throw new RuntimeException("Don't know how to handle " + dataBuffer.getClass().getSimpleName());
        }

        return new Result(width, height, format, result);
    }
}

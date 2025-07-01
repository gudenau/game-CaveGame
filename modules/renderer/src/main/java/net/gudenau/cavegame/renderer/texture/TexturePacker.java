package net.gudenau.cavegame.renderer.texture;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Stream;

// Based on https://github.com/TeamHypersomnia/rectpack2D
public final class TexturePacker<T> {
    public static final class Entry<T> {
        private int x = -1;
        private int y = -1;
        private final int width;
        private final int height;
        private final @NotNull T data;

        public Entry(int width, int height, @NotNull T data) {
            this.width = width;
            this.height = height;
            this.data = data;
        }

        private void update(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int x() {
            return x;
        }

        public int y() {
            return y;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }

        private long area() {
            return (long) width * height;
        }

        @NotNull
        public T data() {
            return data;
        }
    }

    private record Rect(int x, int y, int w, int h) {
        private long area() {
            return (long) w * h;
        }
    }

    private static final int STARTING_WIDTH = 256;
    private static final int STARTING_HEIGHT = 256;

    private final List<Entry<T>> entries = new ArrayList<>();

    private final int padding;

    private int width = STARTING_WIDTH;
    private int height = STARTING_HEIGHT;
    private boolean dirty = true;

    public TexturePacker() {
        this(0);
    }

    public TexturePacker(int padding) {
        this.padding = padding;
    }

    @NotNull
    public Stream<Entry<T>> entries() {
        if(dirty) {
            throw new IllegalStateException("You must call pack first!");
        }

        return entries.stream();
    }

    public void add(int width, int height, @NotNull T data) {
        entries.add(new Entry<>(width, height, data));
        dirty = true;
    }

    public void pack() {
        List<Rect> emptySpaces = new ArrayList<>();
        emptySpaces.add(new Rect(0, 0, width, height));

        entries.stream()
            .filter((entry) -> entry.area() > 0)
            .sorted(Comparator
                .comparingLong((Entry<T> entry) -> entry.area())
                .thenComparingInt(Entry::width)
                .thenComparingInt(Entry::height)
                .reversed()
            )
            .forEachOrdered((entry) -> pack(emptySpaces, entry));

        dirty = false;
    }

    private void pack(List<Rect> emptySpaces, Entry<T> entry) {
        var textureWidth = entry.width + padding * 2;
        var textureHeight = entry.height + padding * 2;

        Rect resultSpace = null;

        for(int i = 0; i < 4; i++) {
            //FIXME This sort should not be required!
            emptySpaces.sort(Comparator.comparingLong(Rect::area).reversed());

            for(int candidateSpaceIndex = emptySpaces.size() - 1; candidateSpaceIndex >= 0; candidateSpaceIndex--) {
                var space = emptySpaces.get(candidateSpaceIndex);
                var freeWidth = space.w() - textureWidth;
                var freeHeight = space.h() - textureHeight;

                if(freeWidth < 0 || freeHeight < 0) {
                    // Too small
                    continue;
                }

                emptySpaces.remove(candidateSpaceIndex);

                if(freeWidth == 0 && freeHeight == 0) {
                    // Exact fit!
                    resultSpace = space;
                    break;
                }

                if(freeHeight == 0) {
                    // Exact height!
                    emptySpaces.add(new Rect(
                        space.x() + textureWidth, space.y(),
                        space.w() - textureWidth, space.h()
                    ));
                    resultSpace = space;
                    break;
                }

                if(freeWidth == 0) {
                    // Exact width!
                    emptySpaces.add(new Rect(
                        space.x(), space.y() + textureHeight,
                        space.w(), space.h() - textureHeight
                    ));
                    resultSpace = space;
                    break;
                }

                Rect smaller;
                Rect larger;

                if(freeWidth > freeHeight) {
                    larger = new Rect(
                        space.x() + textureWidth, space.y(),
                        freeWidth, space.h()
                    );
                    smaller = new Rect(
                        space.x(), space.y() + textureHeight,
                        textureWidth, freeHeight
                    );
                } else {
                    larger = new Rect(
                        space.x(), space.y() + textureHeight,
                        space.w(), freeHeight
                    );
                    smaller = new Rect(
                        space.x() + textureWidth, space.y(),
                        freeWidth, textureHeight
                    );
                }

                resultSpace = space;

                emptySpaces.add(larger);
                emptySpaces.add(smaller);
                break;
            }

            if(resultSpace != null) {
                entry.update(resultSpace.x() + padding, resultSpace.y() + padding);
                return;
            }

            if(width > height) {
                // Make taller
                emptySpaces.addFirst(new Rect(0, height, width, height));

                height *= 2;
            } else {
                // Make wider
                emptySpaces.addFirst(new Rect(width, 0, width, height));

                width *= 2;
            }
        }
    }

    @FunctionalInterface
    public interface RowGetter<T> {
        void get(T data, int[] row, int y);
    }

    public void save(@NotNull RowGetter<T> getter, @NotNull Path destination) {
        throw new UnsupportedOperationException("Implement texture atlas saving");

        /* TODO Texture atlas saving
        if(dirty) {
            throw new IllegalStateException("Can not save a dirty atlas");
        }

        var canvas = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        var graphics = canvas.createGraphics();
        try {
            entries.stream()
                .filter((entry) -> entry.area() > 0)
                .forEach((entry) -> {
                    var entryImage = new BufferedImage(entry.width, entry.height, BufferedImage.TYPE_4BYTE_ABGR);
                    var row = new int[entry.width];
                    for(int y = 0; y < entry.height; y++) {
                        getter.get(entry.data, row, y);
                        entryImage.setRGB(0, y, entry.width, 1, row, 0, entry.width);
                    }
                    graphics.drawImage(entryImage, entry.x, entry.y, null);
                });
        } finally {
            graphics.dispose();
        }

        var filename = destination.getFileName().toString();
        try(var stream = Files.newOutputStream(destination, StandardOpenOption.CREATE)) {
            ImageIO.write(canvas, filename.substring(filename.lastIndexOf('.') + 1), stream);
        } catch(IOException e) {
            throw new RuntimeException("Failed to save atlas to " + destination, e);
        }
         */
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}

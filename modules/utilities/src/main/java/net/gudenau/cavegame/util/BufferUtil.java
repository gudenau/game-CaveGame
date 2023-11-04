package net.gudenau.cavegame.util;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Struct;
import org.lwjgl.system.StructBuffer;

import java.nio.LongBuffer;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Spliterators.*;
import static java.util.Spliterator.SIZED;

public final class BufferUtil {
    private BufferUtil() {
        throw new AssertionError();
    }

    @NotNull
    public static LongStream pointerStream(@NotNull PointerBuffer buffer) {
        return StreamSupport.longStream(pointerSpliterator(buffer), false);
    }

    @NotNull
    public static Spliterator.OfLong pointerSpliterator(@NotNull PointerBuffer buffer) {
        return new AbstractLongSpliterator(buffer.remaining(), SIZED) {
            int offset = buffer.position();
            final int limit = buffer.limit();

            @Override
            public boolean tryAdvance(LongConsumer action) {
                if(offset < limit) {
                    action.accept(buffer.get(offset++));
                    return true;
                }
                return false;
            }
        };
    }

    @NotNull
    public static <B extends Struct<B>, T extends StructBuffer<B, T>> Stream<B> structureStream(@NotNull T buffer) {
        return StreamSupport.stream(structureSpliterator(buffer), false);
    }

    @NotNull
    public static <B extends Struct<B>, T extends StructBuffer<B, T>> Spliterator<B> structureSpliterator(@NotNull T buffer) {
        return new AbstractSpliterator<>(buffer.remaining(), 0){
            private final int limit = buffer.limit();
            private int index = buffer.position();

            @Override
            public boolean tryAdvance(Consumer<? super B> action) {
                if(index < limit) {
                    action.accept(buffer.get(index++));
                    return true;
                }

                return false;
            }
        };
    }

    @NotNull
    public static LongBuffer ofFilled(@NotNull MemoryStack stack, int size, long value) {
        var buffer = stack.mallocLong(size);
        for(int i = 0; i < size; i++) {
            buffer.put(i, value);
        }
        return buffer;
    }

    public static long @NotNull [] toArray(@NotNull LongBuffer buffer) {
        var array = new long[buffer.remaining()];
        buffer.get(buffer.position(), array);
        return array;
    }
}

package net.gudenau.cavegame.util.collection;

import net.gudenau.cavegame.util.SharedLock;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

public sealed class SharedIterable<T> implements Iterable<T> permits SharedCollection {
    protected final SharedLock lock;
    private final Iterable<T> iterable;

    protected SharedIterable(SharedLock lock, Iterable<T> iterable) {
        this.lock = lock;
        this.iterable = iterable;
    }

    @NotNull
    public static <T> SharedIterable<T> of(@NotNull Iterable<T> iterable) {
        if(iterable instanceof SharedIterable<T> shared) {
            return shared;
        } else {
            return new SharedIterable<>(new SharedLock(), iterable);
        }
    }

    @NotNull
    @Override
    public final Iterator<T> iterator() {
        throw new UnsupportedOperationException("SharedIterable doesn't support iterator yet");
    }

    @Override
    public final void forEach(Consumer<? super T> action) {
        lock.read(() -> iterable.forEach(action));
    }

    @Override
    public final Spliterator<T> spliterator() {
        throw new UnsupportedOperationException("SharedIterable doesn't support iterator yet");
    }
}

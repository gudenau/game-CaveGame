package net.gudenau.cavegame.util.collection;

import net.gudenau.cavegame.util.ExclusiveLock;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Supplier;

public final class ThreadQueue<T extends ThreadQueue.Cleaner> implements AutoCloseable {
    @FunctionalInterface
    public interface Cleaner {
        void cleanup();
    }

    @NotNull
    public static <T extends Cleaner> ThreadQueue<T> of(@NotNull Supplier<T> factory) {
        Objects.requireNonNull(factory);
        return new ThreadQueue<>(factory);
    }

    private final ExclusiveLock lock = new ExclusiveLock();
    private final Queue<T> cache = new LinkedList<>();
    private final Supplier<T> factory;

    private ThreadQueue(Supplier<T> factory) {
        this.factory = factory;
    }

    @NotNull
    public T get() {
        return lock.lock(() -> {
            var object = cache.poll();
            if(object == null) {
                object = factory.get();
            }
            return object;
        });
    }

    public void put(@NotNull T object) {
        lock.lock(() -> cache.add(object));
    }

    @Override
    public void close() {
        var exceptions = new ArrayList<Throwable>();

        lock.lock(() -> cache.forEach((closeable) -> {
            try {
                closeable.cleanup();
            } catch(Exception e) {
                exceptions.add(e);
            }
        }));

        if(!exceptions.isEmpty()) {
            var exception = new RuntimeException("Failed to cleanup ThreadQueue");
            exceptions.forEach(exception::addSuppressed);
            throw exception;
        }
    }
}

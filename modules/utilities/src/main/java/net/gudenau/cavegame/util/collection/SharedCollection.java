package net.gudenau.cavegame.util.collection;

import net.gudenau.cavegame.util.SharedLock;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

public sealed class SharedCollection<T> extends SharedIterable<T> implements Collection<T> permits SharedSet {
    private final Collection<T> collection;

    protected SharedCollection(SharedLock lock, Collection<T> collection) {
        super(lock, collection);
        this.collection = collection;
    }

    @NotNull
    public static <T> SharedCollection<T> of(@NotNull Collection<T> collection) {
        if(collection instanceof SharedCollection<T> shared) {
            return shared;
        } else {
            return new SharedCollection<>(new SharedLock(), collection);
        }
    }

    @Override
    public int size() {
        return lock.read(collection::size);
    }

    @Override
    public <T1> T1[] toArray(IntFunction<T1[]> generator) {
        return lock.read(() -> collection.toArray(generator));
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        return lock.write(() -> collection.removeIf(filter));
    }

    @Override
    public Stream<T> stream() {
        throw new UnsupportedOperationException("SharedCollection doesn't support stream yet");
    }

    @Override
    public Stream<T> parallelStream() {
        throw new UnsupportedOperationException("SharedCollection doesn't parallelStream stream yet");
    }

    @Override
    public boolean isEmpty() {
        return lock.read(collection::isEmpty);
    }

    @Override
    public boolean contains(Object o) {
        return lock.read(() -> collection.contains(o));
    }

    @NotNull
    @Override
    public Object @NotNull [] toArray() {
        return lock.read(() -> collection.toArray());
    }

    @NotNull
    @Override
    public <T1> T1 @NotNull [] toArray(@NotNull T1 @NotNull [] a) {
        return lock.read(() -> collection.toArray(a));
    }

    @Override
    public boolean add(T t) {
        return lock.write(() -> collection.add(t));
    }

    @Override
    public boolean remove(Object o) {
        return lock.write(() -> collection.remove(o));
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return lock.read(() -> collection.containsAll(c));
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends T> c) {
        return lock.write(() -> collection.addAll(c));
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return lock.write(() -> collection.removeAll(c));
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return lock.write(() -> collection.retainAll(c));
    }

    @Override
    public void clear() {
        lock.write(collection::clear);
    }
}

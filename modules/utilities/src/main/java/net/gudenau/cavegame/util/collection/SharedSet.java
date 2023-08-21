package net.gudenau.cavegame.util.collection;

import net.gudenau.cavegame.util.SharedLock;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public final class SharedSet<T> extends SharedCollection<T> implements Set<T> {
    private SharedSet(SharedLock lock, Set<T> set) {
        super(lock, set);
    }

    public static <T> SharedSet<T> hashSet() {
        return of(new HashSet<>());
    }

    @NotNull
    public static <T> SharedSet<T> of(@NotNull Set<T> set) {
        if(set instanceof SharedSet<T> shared) {
            return shared;
        } else {
            return new SharedSet<>(new SharedLock(), set);
        }
    }
}

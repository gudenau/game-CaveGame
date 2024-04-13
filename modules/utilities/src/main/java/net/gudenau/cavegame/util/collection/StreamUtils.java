package net.gudenau.cavegame.util.collection;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.stream.Stream;

public final class StreamUtils {
    private StreamUtils() {
        throw new AssertionError();
    }

    @NotNull
    public static <T> Optional<T> findOne(@NotNull Stream<T> stream) {
        var iter = stream.iterator();
        if(!iter.hasNext()) {
            return Optional.empty();
        }
        var value = iter.next();
        return iter.hasNext() ? Optional.empty() : Optional.of(value);
    }
}

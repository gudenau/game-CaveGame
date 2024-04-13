package net.gudenau.cavegame.util;

import net.gudenau.cavegame.util.functional.TriFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.BiFunction;

public final class OptionalUtil {
    private OptionalUtil() {
        throw new AssertionError();
    }

    public static <A, B, Q> Optional<Q> allOf(@NotNull Optional<A> a, @NotNull Optional<B> b, @NotNull BiFunction<A, B, Q> mapper) {
        return a.isPresent() && b.isPresent() ? Optional.ofNullable(mapper.apply(a.get(), b.get())) : Optional.empty();
    }

    public static <A, B, C, Q> Optional<Q> allOf(@NotNull Optional<A> a, @NotNull Optional<B> b, @NotNull Optional<C> c, @NotNull TriFunction<A, B, C, Q> mapper) {
        return a.isPresent() && b.isPresent() && c.isPresent() ?
            Optional.ofNullable(mapper.apply(a.get(), b.get(), c.get())) :
            Optional.empty();
    }
}

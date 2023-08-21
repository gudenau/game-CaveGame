package net.gudenau.cavegame.codec;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public final class PartialResult<T> {
    private final T result;
    private final Supplier<String> error;

    private PartialResult(T result, Supplier<String> error) {
        this.result = result;
        this.error = error;
    }

    @Contract("_ -> new")
    @NotNull
    public static <T> PartialResult<T> of(@NotNull Supplier<String> message) {
        return new PartialResult<>(null, message);
    }

    @Contract("_, _ -> new")
    @NotNull
    public static <T> PartialResult<T> of(@NotNull T result, @NotNull Supplier<String> message) {
        return new PartialResult<>(result, message);
    }

    @NotNull
    public Optional<T> result() {
        return Optional.of(result);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @NotNull
    public T getResult() {
        return result().get();
    }

    public boolean hasResult() {
        return result != null;
    }

    @NotNull
    public String error() {
        return error.get();
    }

    @NotNull
    @Contract("_ -> new")
    public <T2> PartialResult<T2> map(Function<T, T2> mapper) {
        return new PartialResult<>(result().map(mapper).orElse(null), error);
    }

    @NotNull
    @Contract("_ -> new")
    public <T2> PartialResult<T2> flatMap(Function<T, PartialResult<T2>> mapper) {
        if(result != null) {
            var result = mapper.apply(this.result);
            return new PartialResult<>(result.result, () -> error() + ": " + result.error());
        } else {
            return new PartialResult<>(null, error);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartialResult<?> that = (PartialResult<?>) o;
        return Objects.equals(result, that.result) && Objects.equals(error, that.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(result, error);
    }

    @Override
    public String toString() {
        return "PartialResult{" +
            "result=" + result() +
            ", error=" + error() +
            '}';
    }

    @NotNull
    Supplier<String> rawError() {
        return error;
    }
}

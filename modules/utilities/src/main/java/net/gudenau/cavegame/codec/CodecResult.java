package net.gudenau.cavegame.codec;

import net.gudenau.cavegame.util.collection.Either;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class CodecResult<T> {
    private final Either<T, PartialResult<T>> result;

    private CodecResult(Either<T, PartialResult<T>> result) {
        this.result = result;
    }

    public static <T> CodecResult<T> success(T value) {
        return new CodecResult<>(Either.left(value));
    }

    public static <T> CodecResult<T> error(Supplier<String> message) {
        return new CodecResult<>(Either.right(PartialResult.of(message)));
    }

    public static <T> CodecResult<T> error(T partial, Supplier<String> message) {
        return new CodecResult<>(Either.right(PartialResult.of(partial, message)));
    }

    @NotNull
    public Either<T, PartialResult<T>> get() {
        return result;
    }

    @NotNull
    public Optional<T> result() {
        return result.left();
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @NotNull
    public T getResult() {
        return result().get();
    }

    public boolean hasResult() {
        return result.hasLeft();
    }

    public Optional<T> resultOrPartial() {
        return hasResult() ? result() : partial().map(PartialResult::getResult);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public T getResultOrPartial() {
        return resultOrPartial().get();
    }

    @NotNull
    public Optional<PartialResult<T>> partial() {
        return result.right();
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @NotNull
    public PartialResult<T> getPartial() {
        return partial().get();
    }

    public boolean hasPartial() {
        return result.hasRight();
    }

    @NotNull
    public Optional<T> resultOrPartial(Consumer<String> errorHandler) {
        return result.map(
            Optional::of,
            (partial) -> {
                errorHandler.accept(partial.error());
                return partial.result();
            }
        );
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @NotNull
    public T getOrThrow(boolean allowPartial, Consumer<String> errorHandler) {
        return result.map(
            Function.identity(),
            (partial) -> {
                var message = partial.error();
                errorHandler.accept(message);
                if(allowPartial && partial.hasResult()) {
                    return partial.result().get();
                } else {
                    throw new RuntimeException(message);
                }
            }
        );
    }

    public <T2> CodecResult<T2> map(Function<T, T2> mapper) {
        return new CodecResult<>(result.map(
            (result) -> Either.left(mapper.apply(result)),
            (partial) -> {
                if(partial.hasResult()) {
                    return Either.right(PartialResult.of(mapper.apply(partial.result().get()), partial.rawError()));
                } else {
                    return Either.right(PartialResult.of(partial::error));
                }
            }
        ));
    }

    public <T2> CodecResult<T2> flatMap(Function<T, CodecResult<T2>> mapper) {
        return result.map(
            mapper,
            (partial) -> {
                if(partial.hasResult()) {
                    var result = mapper.apply(partial.getResult());
                    if(result.hasResult()) {
                        return result;
                    }

                    Supplier<String> error = () -> partial.error() + ": " + result.getPartial().error();
                    if(result.hasResult()) {
                        return CodecResult.error(result.getPartial().getResult(), error);
                    }
                    return CodecResult.error(error);
                } else {
                    return CodecResult.error(partial.rawError());
                }
            }
        );
    }

    @Override
    public String toString() {
        return result.map(
            (result) -> "CodecResult{result=" + result + '}',
            (partial) -> {
                if(partial.hasResult()) {
                    return "CodecResult{" +
                        "partial=" + partial.result().get() + ',' +
                        "error=" + partial.error() +
                        '}';
                } else {
                    return "CodecResult{" +
                        "error=" + partial.error() +
                        '}';
                }
            });
    }
}

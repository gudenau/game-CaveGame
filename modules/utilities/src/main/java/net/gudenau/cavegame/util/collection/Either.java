package net.gudenau.cavegame.util.collection;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;

public sealed abstract class Either<L, R> permits Either.Left, Either.Right {
    public static <L, R> Either<L, R> left(@NotNull L value) {
        return new Left<>(value);
    }

    public static <L, R> Either<L, R> right(@NotNull R value) {
        return new Right<>(value);
    }

    @NotNull
    public abstract Optional<L> left();

    public abstract boolean hasLeft();

    @NotNull
    public abstract <L2> Either<L2, R> mapLeft(Function<L, L2> mapper);

    @NotNull
    public abstract Optional<R> right();

    public abstract boolean hasRight();

    @NotNull
    public abstract <R2> Either<L, R2> mapRight(Function<R, R2> mapper);

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @NotNull
    public <Q> Q map(Function<L, Q> left, Function<R, Q> right) {
        return hasLeft() ? mapLeft(left).left().get() : mapRight(right).right().get();
    }

    final static class Left<L, R> extends Either<L, R> {
        private final L value;

        private Left(L value) {
            this.value = value;
        }

        @Override
        @NotNull
        public Optional<L> left() {
            return Optional.of(value);
        }

        @Override
        public boolean hasLeft() {
            return true;
        }

        @Override
        public @NotNull <L2> Either<L2, R> mapLeft(Function<L, L2> mapper) {
            return new Left<>(mapper.apply(value));
        }

        @Override
        public @NotNull Optional<R> right() {
            return Optional.empty();
        }

        @Override
        public boolean hasRight() {
            return false;
        }

        @SuppressWarnings("unchecked")
        @Override
        @NotNull
        public <R2> Either<L, R2> mapRight(Function<R, R2> mapper) {
            return (Either<L, R2>) this;
        }
    }

    final static class Right<L, R> extends Either<L, R> {
        private final R value;

        private Right(R value) {
            this.value = value;
        }

        @Override
        @NotNull
        public Optional<L> left() {
            return Optional.empty();
        }

        @Override
        public boolean hasLeft() {
            return false;
        }

        @SuppressWarnings("unchecked")
        @Override
        @NotNull
        public <L2> Either<L2, R> mapLeft(Function<L, L2> mapper) {
            return (Either<L2, R>) this;
        }

        @Override
        @NotNull
        public Optional<R> right() {
            return Optional.of(value);
        }

        @Override
        public boolean hasRight() {
            return true;
        }

        @Override
        @NotNull
        public <R2> Either<L, R2> mapRight(Function<R, R2> mapper) {
            return new Right<>(mapper.apply(value));
        }
    }
}

package net.gudenau.cavegame.gui.value;

import net.gudenau.cavegame.gui.drawing.Font;
import net.gudenau.cavegame.gui.drawing.TextMetrics;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.SequencedCollection;

/// A {@link Value} that can only be set a limited set of values.
public sealed interface UniverseValue<T> extends SeekableValue<T> permits EnumValue {
    /// Gets a read-only view of the set of allowable states.
    ///
    /// @return A read-only view of allowable states
    @Contract(pure = true)
    @NotNull SequencedCollection<T> universe();

    @Contract(pure = true)
    @Override
    default int valueCount() {
        return universe().size();
    }

    /// Gets the first value in the {@link #universe()} of this value.
    ///
    /// @return The first value in the {@link #universe()}
    @Contract(pure = true)
    @Override
    @NotNull
    default T first() {
        return universe().getFirst();
    }

    /// Gets the last value in the {@link #universe()} of this value.
    ///
    /// @return The last value in the {@link #universe()}
    @Contract(pure = true)
    @Override
    @NotNull
    default T last() {
        return universe().getLast();
    }

    /// Sets the current state of this value to the next valid state in the {@link #universe()}. If the currently set
    /// state is the last valid one, the first one is set instead.
    ///
    /// @return The new state of this value
    @NotNull
    default T next() {
        increment();
        return value();
    }

    /// Sets the current state of this value to the previous valid state in the {@link #universe()}. If the currently
    /// set state is the first valid one, the last one is set instead.
    ///
    /// @return The new state of this value
    @NotNull
    default T previous() {
        decrement();
        return value();
    }

    /// Computes the {@link TextMetrics} of every element of this value's {@link #universe()} and returns the
    /// {@link TextMetrics} that will be large enough to contain the result of each elements' {@link Object#toString}.
    ///
    /// @return A {@link TextMetrics} that is large enough to hold any value in this {@link #universe()}
    //TODO Determine if this should be here or elsewhere.
    @Contract(pure = true)
    @NotNull
    default Optional<TextMetrics> metrics(@NotNull Font font) {
        return universe().stream()
            .map(String::valueOf)
            .map(font::metrics)
            .reduce((a, b) -> new TextMetrics(
                a.ascent(),
                Math.max(a.width(), b.width()),
                Math.max(a.height(), b.height())
            ));
    }
}

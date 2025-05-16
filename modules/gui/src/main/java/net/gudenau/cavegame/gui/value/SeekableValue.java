package net.gudenau.cavegame.gui.value;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/// A value that has a set of states that can be uniquely identified by a set of integers. A single integer must map to
/// one value and a value must only map to a single integer. The set of integers must start at 0 and end one before the
/// limit returned in {@link #valueCount()}, so `[0, valueCount())`.
///
/// For example, an {@link Enum}'s {@link Enum#ordinal() ordinal}s would satisfy the requirements of this interface.
public sealed interface SeekableValue<T> extends RangeValue<T> permits IntegerValue, UniverseValue {
    /// Gets the value that is identified by the given int.
    ///
    /// @param index The int to query
    /// @return The value identified by the index
    @Contract(pure = true)
    @NotNull T valueAt(int index);

    /// Gets the count of states that this value can be set to.
    ///
    /// @return The count of states that this value can be set to
    @Contract(pure = true)
    int valueCount();

    /// Gets the integer the maps to the provided state.
    ///
    /// @param value The state to get the index of
    /// @return The index of the provided state
    @Contract(pure = true)
    int indexOf(@NotNull T value);

    /// Gets the index of the current state of this value.
    ///
    /// @return The index of the current state
    @Contract(pure = true)
    default int indexOf() {
        return indexOf(value());
    }

    /// Sets the state of this value to the state identified by provided index.
    ///
    /// @param index The index of the new state
    /// @return The old state
    @NotNull
    default T seek(int index) {
        return value(valueAt(index));
    }

    /// Sets the state to the next allowed state of this value. If the calculated index is out of bounds it wraps
    /// around.
    ///
    /// Equivalent to `seek((indexOf() + 1) % (valueCount() - 1))`
    ///
    /// @return The old state
    @NotNull
    default T increment() {
        return increment(1);
    }

    /// Sets the state to the previous allowed state of this value. If the calculated index is out of bounds it wraps
    /// around.
    ///
    /// Equivalent to
    /// ```Java
    /// var index = indexOf() - 1;
    /// var limit = valueCount() - 1;
    /// if(index < 0) {
    ///     index = limit;
    /// }
    /// return seek(index);
    /// ```
    ///
    /// @return The old state
    @NotNull
    default T decrement() {
        return increment(-1);
    }

    /// Sets the state to the current state minus the provided amount. If the calculated index is out of bounds it wraps
    /// around.
    ///
    /// Equivalent to
    /// ```Java
    /// var index = indexOf() - amount;
    /// var limit = valueCount() - 1;
    /// while(index < 0) {
    ///     index += limit;
    /// }
    /// return seek(index);
    /// ```
    ///
    /// @return The old state
    @NotNull
    default T decrement(int amount) {
        return increment(-amount);
    }

    /// Sets the state to the index of this value plus the amount. If the index is out of bounds it wraps around.
    ///
    /// Equivalent to `seek((indexOf() + amount) % (valueCount() - 1))`
    ///
    /// @return The old state
    @NotNull
    default T increment(int amount) {
        var value = value();
        var index = indexOf(value);
        var count = valueCount();

        index += amount;
        while(index < 0) {
            index += count;
        }
        index %= count;
        return seek(index);
    }
}

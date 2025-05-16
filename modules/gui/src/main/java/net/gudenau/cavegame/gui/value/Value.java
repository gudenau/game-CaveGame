package net.gudenau.cavegame.gui.value;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.SequencedCollection;

/// The base interface for a value that can be represented inside a GUI component. This interface supports event
/// callbacks so the GUI can be updated as values change instead of having to keep track of invalidations manually.
///
/// @param <T> The type of contained value
public sealed interface Value<T> permits MutableValue, RangeValue {
    /// The event callback interface for responding to change events.
    @FunctionalInterface
    interface Event<T> {
        /// Fired when a value is modified to a different value. If the old value is {@link Object#equals equal} to the
        /// new value this is not fired.
        ///
        /// @param value The new value
        /// @param old The old value
        void onChanged(@NotNull T value, @NotNull T old);
    }

    /// Creates a mutable {@link Value} instance with an initial state.
    ///
    /// @param initial The initial state
    /// @param <T> The generic type of this value
    /// @return A new mutable {@link Value<T>}
    @Contract("_ -> new")
    @NotNull
    static <T> Value<T> mutable(@NotNull T initial) {
        return new MutableValue<>(initial);
    }

    /// Creates a mutable {@link Value} from an {@link Enum} that can only contain the enumerated values.
    ///
    /// @param initial The initial value
    /// @param <T> The type of this value
    /// @return A new mutable {@link UniverseValue<T>}
    @Contract("_ -> new")
    @NotNull
    static <T extends Enum<T>> UniverseValue<T> enumeration(@NotNull T initial) {
        return enumeration(initial, List.of(initial.getDeclaringClass().getEnumConstants()));
    }

    /// Creates a mutable {@link Value} from an {@link Enum} that is limited to a specific set of values instead of all
    /// enumerated values.
    ///
    /// @param initial The initial value
    /// @param universe All allowable values
    /// @param <T> The type of this value
    /// @return A new mutable {@link UniverseValue<T>}
    @Contract("_, _ -> new")
    @NotNull
    static <T extends Enum<T>> UniverseValue<T> enumeration(@NotNull T initial, @NotNull SequencedCollection<T> universe) {
        if(!universe.contains(initial)) {
            throw new IllegalArgumentException("initial value (" + initial + ") was not in the universe");
        }

        return new EnumValue<>(initial, universe);
    }

    /// Creates an integer {@link Value} that only allows a specific range of values to be stored.
    ///
    /// @param initial The initial value
    /// @param min The minimum value
    /// @param max The maximum value
    /// @return A new mutable {@link RangeValue<Integer>}
    @NotNull
    static RangeValue<Integer> range(int initial, int min, int max) {
        return new IntegerValue(initial, min, max);
    }

    /// Gets the current state of this {@link Value}
    ///
    /// @return The current value
    @NotNull T value();

    /// Sets the current state of this {@link Value} and returns the old state.
    ///
    /// @param value The new state
    /// @return The old state
    @NotNull T value(@NotNull T value);

    /// Adds a new event listener to this value.
    ///
    /// @param event The event handler
    void registerEvent(@NotNull Event<T> event);

    /// Removes an event listener from this value.
    ///
    /// @param event The event handler
    void deregisterEvent(@NotNull Event<T> event);
}

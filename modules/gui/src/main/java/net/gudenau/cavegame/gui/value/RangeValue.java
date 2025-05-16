package net.gudenau.cavegame.gui.value;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/// A value with a minimum and a maximum allowed state.
public sealed interface RangeValue<T> extends Value<T> permits SeekableValue {
    /// The first valid state of this value.
    ///
    /// @return The first valid state
    @Contract(pure = true)
    @NotNull T first();

    /// The last valid state of this value.
    ///
    /// @return The last valid state
    @Contract(pure = true)
    @NotNull T last();
}

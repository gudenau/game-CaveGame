package net.gudenau.cavegame.gui.value;

import org.jetbrains.annotations.NotNull;

public sealed interface RangeValue<T> extends Value<T> permits SeekableValue {
    @NotNull T first();
    @NotNull T last();
}

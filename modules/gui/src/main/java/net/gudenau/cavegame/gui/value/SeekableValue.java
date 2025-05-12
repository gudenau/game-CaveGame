package net.gudenau.cavegame.gui.value;

import org.jetbrains.annotations.NotNull;

public sealed interface SeekableValue<T> extends RangeValue<T> permits IntegerValue, UniverseValue {
    @NotNull T valueAt(int index);
    int valueCount();
    int indexOf(@NotNull T value);

    default int indexOf() {
        return indexOf(value());
    }

    @NotNull
    default T seek(int index) {
        return value(valueAt(index));
    }

    @NotNull
    default T increment() {
        return increment(1);
    }

    @NotNull
    default T decrement() {
        return increment(-1);
    }

    @NotNull
    default T decrement(int amount) {
        return increment(-amount);
    }

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

package net.gudenau.cavegame.gui.value;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class IntegerValue implements SeekableValue<Integer> {
    private final Set<Event<Integer>> handlers = new HashSet<>();
    private final int max;
    private final int min;

    private int value;

    public IntegerValue(int initial, int max, int min) {
        if(min > max) {
            throw new IllegalArgumentException("Max (" + max + ") is smaller than min (" + min + ')');
        }

        this.max = max;
        this.min = min;
        this.value = Math.clamp(initial, min, max);
    }

    @Override
    @NotNull
    public Integer value() {
        return value;
    }

    @Override
    @NotNull
    public Integer first() {
        return min;
    }

    @Override
    @NotNull
    public Integer last() {
        return max;
    }

    @Override
    @NotNull
    public Integer value(@NotNull Integer value) {
        final int clamped = Math.clamp(value, min, max);

        if(clamped == this.value) {
            return clamped;
        }

        var old = this.value;
        this.value = clamped;
        handlers.forEach((handler) -> handler.onChanged(clamped, old));
        return old;
    }

    @Override
    public void registerEvent(@NotNull Event<Integer> event) {
        handlers.add(event);
    }

    @Override
    public void deregisterEvent(@NotNull Event<Integer> event) {
        handlers.remove(event);
    }

    @Override
    @NotNull
    public Integer valueAt(int index) {
        Objects.checkIndex(index, valueCount());

        var existing = this.value;
        this.value = index + min;
        return existing;
    }

    @Override
    public int valueCount() {
        var range = max - min;
        if(range < 0) {
            throw new AssertionError("Range was negative for min " + min + " and max " + max + '?');
        }
        return range + 1;
    }

    @Override
    public int indexOf(@NotNull Integer value) {
        return value - min;
    }
}

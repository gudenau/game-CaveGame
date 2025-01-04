package net.gudenau.cavegame.gui.value;

import org.jetbrains.annotations.NotNull;

import java.util.*;

final class EnumValue<T extends Enum<T>> extends MutableValue<T> implements UniverseValue<T> {
    @NotNull
    private final List<T> universe;

    public EnumValue(@NotNull T value, @NotNull SequencedCollection<T> universe) {
        super(value);
        // Make sure it's immutable and sequenced
        this.universe = universe.stream()
            .distinct()
            .toList();
    }

    @Override
    @NotNull
    public T value(@NotNull T value) {
        if(!universe.contains(value)) {
            throw new IllegalArgumentException("Value " + value + " was not in the universe");
        }

        return super.value(value);
    }

    @NotNull
    @Override
    public SequencedCollection<T> universe() {
        return universe;
    }
}

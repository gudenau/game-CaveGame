package net.gudenau.cavegame.gui.value;

import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.SequencedCollection;
import java.util.Set;

public sealed interface Value<T> permits MutableValue, UniverseValue {
    @FunctionalInterface
    interface Event<T> {
        void onChanged(@NotNull T value, @NotNull T old);
    }

    @NotNull
    static <T> Value<T> mutable(@NotNull T initial) {
        return new MutableValue<>(initial);
    }

    @NotNull
    static <T extends Enum<T>> UniverseValue<T> enumeration(@NotNull T initial) {
        return enumeration(initial, List.of(initial.getDeclaringClass().getEnumConstants()));
    }

    @NotNull
    static <T extends Enum<T>> UniverseValue<T> enumeration(@NotNull T initial, @NotNull SequencedCollection<T> universe) {
        if(!universe.contains(initial)) {
            throw new IllegalArgumentException("initial value (" + initial + ") was not in the universe");
        }

        return new EnumValue<>(initial, universe);
    }

    @NotNull T value();
    @NotNull T value(@NotNull T value);

    void registerEvent(@NotNull Event<T> event);
    void deregisterEvent(@NotNull Event<T> event);
}

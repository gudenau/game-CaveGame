package net.gudenau.cavegame.gui.value;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

sealed class MutableValue<T> implements Value<T> permits EnumValue {
    private final Set<Event<T>> handlers = new HashSet<>();

    @NotNull
    private T value;

    public MutableValue(@NotNull T value) {
        this.value = value;
    }

    @Override
    @NotNull
    public T value() {
        return value;
    }

    @Override
    @NotNull
    public T value(@NotNull T value) {
        var old = this.value;
        if(old.equals(value)) {
            return old;
        }
        this.value = value;
        handlers.forEach((handler) -> handler.onChanged(value, old));
        return old;
    }

    @Override
    public void registerEvent(@NotNull Event<T> event) {
        handlers.add(event);
    }

    @Override
    public void deregisterEvent(@NotNull Event<T> event) {
        handlers.remove(event);
    }
}

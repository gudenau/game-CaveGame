package net.gudenau.cavegame.util;

import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public final class CallbackRegistry<T> extends Registry<T> {
    private final BiConsumer<Identifier, T> callback;

    CallbackRegistry(BiConsumer<Identifier, T> callback) {
        super();

        this.callback = callback;
    }

    @Override
    public void register(@NotNull Identifier name, @NotNull T object) {
        super.register(name, object);

        callback.accept(name, object);
    }
}

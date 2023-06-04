package net.gudenau.cavegame.util;

import net.gudenau.cavegame.resource.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

/**
 * A {@link Registry} implementation that fires a callback every time a new value is successfully registered.
 *
 * @param <T> The type of this registry
 */
public final class CallbackRegistry<T> extends Registry<T> {
    /**
     * The callback to invoke when a new value is registered.
     */
    private final BiConsumer<Identifier, T> callback;

    public CallbackRegistry(BiConsumer<Identifier, T> callback) {
        super();

        this.callback = callback;
    }

    @Override
    public void register(@NotNull Identifier name, @NotNull T object) {
        super.register(name, object);

        callback.accept(name, object);
    }
}

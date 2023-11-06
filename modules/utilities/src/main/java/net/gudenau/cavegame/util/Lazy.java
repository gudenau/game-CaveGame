package net.gudenau.cavegame.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A basic "lazy" object holder, defers expensive initialization operations until the value is required.<br>
 * <br>
 * Thread safe
 *
 * @param <T> The type of the held value
 */
public final class Lazy<T> {
    /**
     * The factory that performs the initialization, set to null after it's used.
     */
    @Nullable
    private Supplier<@NotNull T> factory;

    /**
     * The value of this holder, null until it gets initialized.
     */
    @Nullable
    private volatile T value;

    private Lazy(@NotNull Supplier<T> factory) {
        this.factory = factory;
    }

    /**
     * Creates a new lazy instance with the provided factory.
     *
     * @param factory The factory to use
     * @return The new Lazy instance
     * @param <T> The type of the held value
     */
    @Contract("_ -> new")
    public static <T> Lazy<T> of(@NotNull Supplier<@NotNull T> factory) {
        return new Lazy<>(Objects.requireNonNull(factory, "factory can't be null"));
    }

    /**
     * Creates a new lazy supplier with the provided factory.
     * <p>
     * Equivalent to:
     * {@snippet : Lazy.of(supplier)::get }
     *
     * @param factory The factory to use
     * @return The new Lazy instance
     * @param <T> The type of the held value
     */
    @Contract("_ -> new")
    public static <T> Supplier<T> supplier(@NotNull Supplier<@NotNull T> factory) {
        return of(factory)::get;
    }

    /**
     * Gets the held value or calls the factory for it if not yet initialized.
     *
     * @return The held value
     */
    @SuppressWarnings("DataFlowIssue")
    @NotNull
    public T get() {
        if(value == null) {
            synchronized (this) {
                if(value == null) {
                    value = factory.get();
                    factory = null;
                }
            }
        }

        return value;
    }
}

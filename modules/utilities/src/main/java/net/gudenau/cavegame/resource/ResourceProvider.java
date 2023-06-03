package net.gudenau.cavegame.resource;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * Resolves {@link Identifier}s to their corresponding {@link Path}s on behalf of the {@link ResourceLoader}.
 */
public interface ResourceProvider extends AutoCloseable {
    /**
     * Attempts to resolve an {@link Identifier} to its {@link Path}.
     *
     * @param identifier The identifier to resolve
     * @return The resolved {@link Path} or null if failed
     */
    @Nullable Path path(@NotNull Identifier identifier);
}

package net.gudenau.cavegame.resource;

import net.gudenau.cavegame.logger.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Resolves {@link Identifier}s to their corresponding {@link Path}s on behalf of the {@link ResourceLoader}.
 */
public interface ResourceProvider extends AutoCloseable {
    int UNLIMITED_DEPTH = -1;

    /**
     * Attempts to resolve an {@link Identifier} to its {@link Path}.
     *
     * @param identifier The identifier to resolve
     * @return The resolved {@link Path} or null if failed
     */
    @Nullable Path path(@NotNull Identifier identifier);

    @NotNull
    default Stream<Identifier> walk(@NotNull Identifier identifier, int depth) {
        var path = path(identifier);
        if(!Files.exists(path)) {
            return Stream.empty();
        }

        Stream<Path> stream;
        try {
            if(depth == UNLIMITED_DEPTH) {
                stream = Files.walk(path);
            } else {
                stream = Files.walk(path, depth);
            }
        } catch(IOException e) {
            Logger.forClass(ClassPathResourceProvider.class).error("Failed to walk identifier " + identifier, e);
            stream = Stream.empty();
        }

        return stream.filter((element) -> !element.equals(path))
            .map((element) -> {
            var relative = path.relativize(element)
                .toString();

            if(File.separatorChar == '\\') {
                relative = relative.replaceAll("\\\\", "/");
            }

            return identifier.appendPath(relative);
        });
    }
}

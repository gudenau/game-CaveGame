package net.gudenau.cavegame.resource;

import net.gudenau.cavegame.util.Treachery;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.invoke.MethodType;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Map;

/**
 * A simple {@link ResourceProvider} implementation that uses the jar/directory that a class was loaded from as its
 * root.<br>
 * <br>
 * TODO Directory implementation.
 */
public sealed abstract class ClassPathResourceProvider implements ResourceProvider permits ClassPathResourceProvider.Archive {
    /**
     * Creates a new resource provider from the provided class.
     *
     * @param type The class to use
     * @return A new ClassPathResourceProvider instance
     */
    @Contract("_ -> new")
    public static ResourceProvider of(@NotNull Class<?> type) {
        Path path;
        try {
            path = Paths.get(type.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to find Path of " + Treachery.longClassName(type), e);
        }

        if(Files.isRegularFile(path)) {
            try {
                return new Archive(path);
            } catch (IOException e) {
                throw new RuntimeException("Failed to open archive " + path + " for " + Treachery.longClassName(type));
            }
        } else {
            throw new RuntimeException("Not implemented");
        }
    }

    /**
     * The archive based implementation.
     */
    public static final class Archive extends ClassPathResourceProvider {
        /**
         * The {@link FileSystem} object for this provider.
         */
        @NotNull
        private final FileSystem fileSystem;

        private Archive(@NotNull Path path) throws IOException {
            fileSystem = FileSystems.newFileSystem(path, Map.of());
            // Try to make it read-only, fails safe.
            Treachery.tryInvoke(fileSystem, "setReadOnly", MethodType.methodType(void.class));
        }

        @Override
        public Path path(@NotNull Identifier identifier) {
            return fileSystem.getPath(identifier.namespace(), identifier.path());
        }

        @Override
        public void close() throws IOException {
            fileSystem.close();
        }
    }
}

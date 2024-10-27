package net.gudenau.cavegame.resource;

import net.gudenau.cavegame.util.MiscUtils;
import net.gudenau.cavegame.util.Treachery;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.invoke.MethodType;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A simple {@link ResourceProvider} implementation that uses the jar/directory that a class was loaded from as its
 * root.
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
            throw new RuntimeException("Failed to find Path of " + MiscUtils.longClassName(type), e);
        }

        if(Files.isRegularFile(path)) {
            try {
                return new Archive(path);
            } catch (IOException e) {
                throw new RuntimeException("Failed to open archive " + path + " for " + MiscUtils.longClassName(type));
            }
        } else {
            try {
                return new DirectoryTree(path);
            } catch (IOException e) {
                throw new RuntimeException("Failed to discover dir tree " + path + " for " + MiscUtils.longClassName(type));
            }
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

    /**
     * The file tree based implementation.
     */
    private static class DirectoryTree implements ResourceProvider {
        private final Set<Path> paths;

        /**
         * Creates a new DirectoryTree instance and guesses where the resources are saved
         * based on the path provided.
         *
         * @param path The path the holds the classes
         * @throws IOException If the resource path could not be successfully guessed
         */
        private DirectoryTree(Path path) throws IOException {
            var resourcePath = resolveResourcePath(path);
            paths = Set.of(path, resourcePath);
        }

        @NotNull
        private Path resolveResourcePath(@NotNull Path path) throws IOException {
            var ideaPath = path.resolveSibling("resources");
            var gradlePath = path.getParent().getParent().resolveSibling(Path.of("resources", "main"));
            return Stream.of(ideaPath, gradlePath)
                .filter(Files::isDirectory)
                .findFirst()
                .orElseThrow(() -> new IOException("Failed to find resources for " + path));
        }

        @Override
        @Nullable
        public Path path(@NotNull Identifier identifier) {
            var path = Path.of(identifier.namespace(), identifier.path());
            return paths.stream()
                .map((p) -> p.resolve(path))
                .filter(Files::exists)
                .findAny()
                .orElse(null);
        }

        @Override
        public void close() {}
    }
}

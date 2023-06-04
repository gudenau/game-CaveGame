package net.gudenau.cavegame.resource;

import net.gudenau.cavegame.config.Config;
import net.gudenau.cavegame.util.SharedLock;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * The core of resource loading, manages access to namespaces and will manage access to resource packs when implemented.
 * <br>
 *
 * Thread safe.
 */
public final class ResourceLoader {

    /**
     * The map of providers, ensure to use {@link #lock} because this may be accessed by multiple threads.
     */
    private static final Map<String, ResourceProvider> providers = new HashMap<>();
    private static final SharedLock lock = new SharedLock();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(ResourceLoader::close, "ResourceLoader cleanup"));
    }

    private ResourceLoader() {}

    /**
     * Registers a {@link ResourceProvider} to a new namespace.<br>
     * <br>
     * Namespaces follow the same rules as they do in {@link Identifier}.
     *
     * @param namespace The namespace to register
     * @param provider The provider to register
     */
    public static void registerProvider(@NotNull String namespace, @NotNull ResourceProvider provider) {
        Identifier.validateNamespace(namespace);
        Objects.requireNonNull(provider, "provider can't be null");

        lock.write(() -> {
            if(providers.putIfAbsent(namespace, provider) != null) {
                throw new IllegalStateException(namespace + " was already registered by provider " + providers.get(namespace));
            }
        });
    }


    /**
     * Resolves a {@link Path} in the registered {@link ResourceProvider}s.
     *
     * @param identifier The identifier to resolve
     * @return The resolved {@link Path}
     * @throws FileNotFoundException If the {@link Path} could not be resolved
     */
    private static Path path(Identifier identifier) throws FileNotFoundException {
        Objects.requireNonNull(identifier, "identifier can't be null");

        // Idea is being a little too aggressive here, safe to ignore.
        @SuppressWarnings("resource")
        var provider = lock.read(() -> providers.get(identifier.namespace()));
        if(provider == null) {
            throw new FileNotFoundException("namespace " + identifier.namespace() + " does not have a resource provider!");
        }

        var path = provider.path(identifier);
        if(path == null || !Files.exists(path)) {
            throw new FileNotFoundException("resource " + identifier + " could not be found");
        }
        return path;
    }

    /**
     * Opens a read-only channel to the provided identifier.
     *
     * @param identifier The identifier to open
     * @return The opened channel
     * @throws IOException If the channel could not be opened
     * @throws FileNotFoundException If the identfier could not be resolved
     */
    @NotNull
    public static SeekableByteChannel channel(@NotNull Identifier identifier) throws IOException {
        return Files.newByteChannel(path(identifier), StandardOpenOption.READ);
    }

    /**
     * Opens a UTF-8 reader to the provided identifier.
     *
     * @param identifier The identifier to open
     * @return The opened reader
     * @throws IOException If the reader could not be opened
     * @throws FileNotFoundException If the identfier could not be resolved
     */
    @NotNull
    public static BufferedReader reader(@NotNull Identifier identifier) throws IOException {
        return Files.newBufferedReader(path(identifier));
    }

    /**
     * Opens an {@link InputStream} to the provided identifier.
     *
     * @param identifier The identifier to open
     * @return The opened stream
     * @throws IOException If the stream could not be opened
     * @throws FileNotFoundException If the identfier could not be resolved
     */
    @NotNull
    public static InputStream stream(@NotNull Identifier identifier) throws IOException {
        return Files.newInputStream(path(identifier));
    }

    public static void close() {
        Set<Throwable> exceptions = new HashSet<>();
        lock.write(() -> {
            for (var provider : providers.values()) {
                try {
                    provider.close();
                } catch (Exception e) {
                    exceptions.add(e);
                }
            }
            providers.clear();
        });

        if(!exceptions.isEmpty()) {
            var exception = new RuntimeException("Failed to close all providers");
            exceptions.forEach(exception::addSuppressed);
            throw exception;
        }
    }

    /**
     * Reads the entire contents of the file pointed to by the identifier into a native buffer. Ensure that the buffer
     * is freed via {@link MemoryUtil#memFree(Buffer)} otherwise you will leak memory!
     *
     * @param identifier The identifier to read
     * @return The created buffer
     * @throws IOException if there was an error opening or reading the file
     */
    @NotNull
    public static ByteBuffer buffer(Identifier identifier) throws IOException {
        try(var channel = channel(identifier)) {
            var length = channel.size();
            if(length > Config.MAX_BUFFER_SIZE.get()) {
                throw new IOException("Failed to read " + identifier + " into native buffer: File is too large");
            }

            var buffer = MemoryUtil.memAlloc((int) length);
            while(buffer.hasRemaining()) {
                if(channel.read(buffer) <= 0) {
                    throw new IOException("Unexpected end of stream while reading " + identifier);
                }
            }
            return buffer.flip();
        }
    }
}

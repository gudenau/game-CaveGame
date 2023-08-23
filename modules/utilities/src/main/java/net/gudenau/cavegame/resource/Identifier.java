package net.gudenau.cavegame.resource;

import net.gudenau.cavegame.codec.Codec;
import net.gudenau.cavegame.codec.CodecResult;
import net.gudenau.cavegame.codec.ops.Operations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * A basic identifier implementation based off of namespaces and paths.<br>
 * <br>
 * Namespaces have to match the pattern: ^[a-z][a-z0-9_]*$
 * Paths have to match the pattern: ^[a-z][a-z0-9_/\\.]*$
 *
 * @param namespace The namespace to use
 * @param path The path to use
 */
public record Identifier(
    @NotNull String namespace,
    @NotNull String path
) {
    public static final Codec<Identifier> CODEC = new Codec.Impartial<>() {
        @Override
        public <R> CodecResult<R> encode(Operations<R> operations, Identifier input) {
            return CodecResult.success(operations.fromString(input.toString()));
        }

        @Override
        public <R> CodecResult<Identifier> decode(Operations<R> operations, R input) {
            return operations.toString(input).flatMap((string) -> {
                try {
                    return CodecResult.success(new Identifier(string));
                } catch (Throwable e) {
                    return CodecResult.error(() -> '"' + string + "\" is not a valid identifier");
                }
            });
        }

        @Override
        @NotNull
        public Class<Identifier> type() {
            return Identifier.class;
        }
    };

    /**
     * The namespace predicate used to validate identifiers.
     */
    @NotNull
    private static final Predicate<String> NAMESPACE_PREDICATE = Pattern.compile("^[a-z][a-z0-9_]*$").asMatchPredicate();

    /**
     * The path predicate used to validate identifiers.
     */
    @NotNull
    private static final Predicate<String> PATH_PREDICATE = Pattern.compile("^[a-z][a-z0-9_/\\\\.]*$").asMatchPredicate();

    /**
     * The "default" namespace of "cave_game".
     */
    @NotNull
    public static final String CAVEGAME_NAMESPACE = "cave_game";

    public Identifier {
        validateNamespace(namespace);
        validatePath(path);
    }

    /**
     * Creates a new identifier in the form of "namespace:path".
     *
     * @param identifier The identifier to parse
     */
    public Identifier(@NotNull String identifier) {
        //TODO Fix this when JEP 447 happens
        this(
            Objects.requireNonNull(identifier, "identifier can't be null").split(":", 2)[0],
            identifier.split(":", 2)[1]
        );
    }

    /**
     * Adds the provided string to the beginning of this path, followed by a forward slash.
     *
     * @param prefix The prefix to add
     * @return The new identifier
     */
    @NotNull
    public Identifier prefixPath(@NotNull String prefix) {
        validatePath(prefix);
        return new Identifier(namespace, prefix + '/' + path);
    }

    /**
     * Adds a string to the end of this path.
     *
     * @param suffix The suffix to add
     * @return The new identifier
     */
    @NotNull
    public Identifier append(@NotNull String suffix) {
        return new Identifier(namespace, path + suffix);
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }

    /**
     * Validates that a namespace is legal for use in an identifier.
     *
     * @param namespace The namespace to check
     */
    public static void validateNamespace(@NotNull String namespace) {
        Objects.requireNonNull(namespace, "namespace can't be null");
        if(!NAMESPACE_PREDICATE.test(namespace)) {
            throw new IllegalArgumentException("namespace " + namespace + " is illegal");
        }
    }

    /**
     * Validates that a path is legal for use in an identifier.
     *
     * @param path The path to check
     */
    public static void validatePath(@NotNull String path) {
        Objects.requireNonNull(path, "path can't be null");
        if(!PATH_PREDICATE.test(path)) {
            throw new IllegalArgumentException("path " + path + " is illegal");
        }
    }

    /**
     * Gets the filename from the path of this identifier. The filename is the contents of the path after the final '/'
     * or the entire path if there is no `/`.
     *
     * @return The filename of this identifier
     */
    @NotNull
    public String filename() {
        int index = path.lastIndexOf('/');
        if(index == -1) {
            return path;
        } else {
            return path.substring(index + 1);
        }
    }

    /**
     * Gets the directories of the path of this identifier, if they are present. The directories of this path are
     * everything that is before the last '/', if present.
     *
     * @return The directories of this namespace or null if absent
     */
    @Nullable
    public String directory() {
        int index = path.lastIndexOf('/');
        if(index == -1) {
            return null;
        } else {
            return path.substring(0, index);
        }
    }

    /**
     * Gets the extension from the filename of the path in this identifier. The file extension is the contents of the
     * {@link #filename() filename} past the last '.', if present. If there is no extension (I.E. "cave_game:file") this
     * will return 'null', if the extension is empty (I.E. "cave_game:file.") this method will return an empty string.
     *
     * @return The extension if present, null if absent
     */
    @Nullable
    public String extension() {
        var fileName = filename();
        int index = fileName.lastIndexOf('.');
        if(index == -1) {
            return null;
        } else {
            return fileName.substring(index + 1);
        }
    }

    /**
     * Sets the extension of this identifier and returns a new instance. If the current identifier already has an
     * extension this method will fail.
     *
     * @param extension The extension to use
     * @return The new identifier
     * @throws IllegalStateException if the identifier already had an extension
     */
    @NotNull
    public Identifier extension(@NotNull String extension) {
        Objects.requireNonNull(extension, "extension can't be null");

        if(extension() != null) {
            throw new IllegalStateException("Identifier " + this + " already has an extension");
        }

        return append("." + extension);
    }

    @NotNull
    public Identifier normalize(@NotNull String prefix, @NotNull String suffix) {
        return new Identifier(namespace, prefix + '/' + path + suffix);

        /*
        var filename = filename();
        var directory = directory();
        var namespace = namespace();
        if(directory == null) {
            return new Identifier(namespace, prefix + "/" + filename + suffix);
        } else {
            return new Identifier(namespace, prefix + "/" + directory + "/" + filename + suffix);
        }
         */
    }
}

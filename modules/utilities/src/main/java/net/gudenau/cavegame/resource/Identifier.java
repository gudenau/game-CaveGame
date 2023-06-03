package net.gudenau.cavegame.resource;

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
    /**
     * The namespace predicate used to validate identifiers.
     */
    private static final Predicate<String> NAMESPACE_PREDICATE = Pattern.compile("^[a-z][a-z0-9_]*$").asMatchPredicate();

    /**
     * The path predicate used to validate identifiers.
     */
    private static final Predicate<String> PATH_PREDICATE = Pattern.compile("^[a-z][a-z0-9_/\\\\.]*$").asMatchPredicate();

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

    @NotNull
    public String filename() {
        int index = path.lastIndexOf('/');
        if(index == -1) {
            return path;
        } else {
            return path.substring(index + 1);
        }
    }

    @Nullable
    public String directory() {
        int index = path.lastIndexOf('/');
        if(index == -1) {
            return null;
        } else {
            return path.substring(0, index);
        }
    }

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

    @NotNull
    public Identifier extension(@NotNull String extension) {
        Objects.requireNonNull(extension, "extension can't be null");

        if(extension() != null) {
            throw new IllegalStateException("Identifier " + this + " already has an extension");
        }

        return append("." + extension);
    }
}

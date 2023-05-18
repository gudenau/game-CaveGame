package net.gudenau.cavegame.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A basic registry, keeps tracks of the numeric ids, the identifier based names and values of objects.<br>
 * <br>
 * Thread safe.
 *
 * @param <T> The type of the registry
 */
public sealed class Registry<T> permits CallbackRegistry {
    /**
     * A registry entry.
     *
     * @param name The name of the entry
     * @param id The numeric id of the entry
     * @param object The value of the entry
     * @param <T> The type of the entry
     */
    private record Entry<T>(
        @NotNull Identifier name,
        @Range(from = 0, to = Integer.MAX_VALUE) int id,
        @NotNull T object
    ){}

    /**
     * A map of all entries indexed by their names.
     */
    private final Map<Identifier, Entry<T>> nameToEntry = new Object2ObjectOpenHashMap<>();
    /**
     * A map of all entries indexed by their ids.
     */
    private final Int2ObjectMap<Entry<T>> idToEntry = new Int2ObjectOpenHashMap<>();
    /**
     * A map of all entries indexed by their values.
     */
    private final Map<T, Entry<T>> objectToEntry = new Object2ObjectOpenHashMap<>();

    /**
     * The shared lock used to ensure this registry is thread safe.
     */
    private final SharedLock lock = new SharedLock();

    protected Registry() {}

    /**
     * Registers a new value in this registry.
     *
     * @param name The name of the object
     * @param object THe value to register
     */
    public void register(@NotNull Identifier name, @NotNull T object) {
        Objects.requireNonNull(name, "name can't be null");
        Objects.requireNonNull(object, "object can't be null");

        lock.write(() -> {
            if(nameToEntry.containsKey(name)) {
                throw new IllegalArgumentException("Registry already contains name " + name);
            }
            if(objectToEntry.containsKey(object)) {
                throw new IllegalArgumentException("Registry already contains value " + object + " with the name " + objectToEntry.get(object).id());
            }

            var id = nameToEntry.size();
            var entry = new Entry<>(name, id, object);
            nameToEntry.put(name, entry);
            idToEntry.put(id, entry);
            objectToEntry.put(object, entry);
        });
    }

    /**
     * Gets all of the entries in this registry in a stream.
     *
     * @return The stream of entries
     */
    public Stream<Map.Entry<Identifier, T>> entries() {
        return lock.read(() -> Set.copyOf(nameToEntry.entrySet()))
            .stream()
            .map((entry) -> Map.entry(entry.getKey(), entry.getValue().object()));
    }

    /**
     * Gets the value associated with a name.
     *
     * @param name The name to look up
     * @return The value or empty if it doesn't exist
     */
    @NotNull
    public Optional<T> object(@NotNull Identifier name) {
        Objects.requireNonNull(name, "name can't be null");

        return lock.read(() -> Optional.ofNullable(nameToEntry.get(name)).map(Entry::object));
    }

    /**
     * Gets the value associated with a numeric id.
     *
     * @param id The id to look up
     * @return The value or empty if it doesn't exist
     */
    @NotNull
    public Optional<T> object(@Range(from = 0, to = Integer.MAX_VALUE) int id) {
        // Idea is being a little too sensitive here, I'm doing error checking because mistakes happen
        //noinspection ConstantValue
        if(id < 0) {
            throw new IllegalArgumentException("id can't be negative");
        }

        return lock.read(() -> Optional.ofNullable(idToEntry.get(id)).map(Entry::object));
    }

    /**
     * Gets the numeric id associated with a name.
     *
     * @param name The name to look up
     * @return The numeric id or empty if it doesn't exist
     */
    @NotNull
    public OptionalInt id(@NotNull Identifier name) {
        Objects.requireNonNull(name, "name can't be null");

        return lock.read(() -> {
            var entry = nameToEntry.get(name);
            return entry == null ? OptionalInt.empty() : OptionalInt.of(entry.id());
        });
    }

    /**
     * Gets the numeric id associated with a object.
     *
     * @param object The object to look up
     * @return The numeric id or empty if it doesn't exist
     */
    @NotNull
    public OptionalInt id(@NotNull T object) {
        Objects.requireNonNull(object, "object can't be null");

        return lock.read(() -> {
            var entry = objectToEntry.get(object);
            return entry == null ? OptionalInt.empty() : OptionalInt.of(entry.id());
        });
    }

    /**
     * Gets the name associated with a numeric id.
     *
     * @param id The id to look up
     * @return The name or empty if it doesn't exist
     */
    @NotNull
    public Optional<Identifier> name(@Range(from = 0, to = Integer.MAX_VALUE) int id) {
        // Idea is being a little too sensitive here, I'm doing error checking because mistakes happen
        //noinspection ConstantValue
        if(id < 0) {
            throw new IllegalArgumentException("id can't be negative");
        }

        return lock.read(() -> Optional.ofNullable(idToEntry.get(id)).map(Registry.Entry::name));
    }

    /**
     * Gets the name associated with an object.
     *
     * @param object The object to look up
     * @return The name or empty if it doesn't exist
     */
    @NotNull
    public Optional<Identifier> name(@NotNull T object) {
        Objects.requireNonNull(object, "object can't be null");

        return lock.read(() -> Optional.ofNullable(objectToEntry.get(object)).map(Registry.Entry::name));
    }
}

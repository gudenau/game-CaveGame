package net.gudenau.cavegame;

import net.gudenau.cavegame.ai.JobType;
import net.gudenau.cavegame.material.Material;
import net.gudenau.cavegame.tile.Tile;
import net.gudenau.cavegame.util.CallbackRegistry;
import net.gudenau.cavegame.resource.Identifier;
import net.gudenau.cavegame.util.Registry;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

/**
 * All of the default registries for the game.
 */
public final class Registries {
    private Registries() {
        throw new AssertionError();
    }

    /**
     * The registry of all registries.
     */
    // Does the registry of all registry contain itself?
    public static final Registry<Registry<?>> REGISTRY = new Registry<>();
    static {
        // Turns out it does, nifty.
        REGISTRY.register(new Identifier(CaveGame.NAMESPACE, "registry"), REGISTRY);
    }

    /**
     * The tile registry.
     */
    public static final Registry<Tile> TILE = registry("tile");

    /**
     * The resource registry.
     */
    public static final Registry<Material> RESOURCE = registry("resource");

    public static final Registry<JobType<?>> JOB_TYPE = registry("job_type");

    /**
     * Creates and registers a new registry.
     *
     * @param name The name of the registry
     * @return The new registry instance
     * @param <T> The type of the registry
     */
    @Contract("_ -> new")
    @NotNull
    private static <T> Registry<T> registry(String name) {
        var registry = new Registry<T>();
        REGISTRY.register(new Identifier(CaveGame.NAMESPACE, name), registry);
        return registry;
    }

    @Contract("_, _ -> new")
    @NotNull
    private static <T> Registry<T> callbackRegistry(String name, BiConsumer<Identifier, T> callback) {
        var registry = new CallbackRegistry<T>(callback);
        REGISTRY.register(new Identifier(CaveGame.NAMESPACE, name), registry);
        return registry;
    }
}

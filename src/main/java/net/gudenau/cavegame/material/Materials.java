package net.gudenau.cavegame.material;

import net.gudenau.cavegame.CaveGame;
import net.gudenau.cavegame.resource.Identifier;
import net.gudenau.cavegame.Registries;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * All of the resource types in this game.
 */
public final class Materials {
    private Materials() {
        throw new AssertionError();
    }

    /**
     * The resource instance for ore
     */
    @NotNull
    public static final Material ORE = register("ore", new Material());

    /**
     * Registers a resource under the "cave_game" namespace.
     *
     * @param name The name of the resource
     * @param resource The instance of the resource
     * @return The passed resource
     * @param <T> The type of the resource
     */
    @NotNull
    @Contract("_, _ -> param2")
    private static <T extends Material> T register(@NotNull String name, @NotNull T resource) {
        Registries.RESOURCE.register(new Identifier(CaveGame.NAMESPACE, name), resource);
        return resource;
    }
}

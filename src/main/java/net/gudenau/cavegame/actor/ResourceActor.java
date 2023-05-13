package net.gudenau.cavegame.actor;

import net.gudenau.cavegame.level.Level;
import net.gudenau.cavegame.material.Material;
import net.gudenau.cavegame.util.TilePos;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * The actor that contains dropped resources.
 */
public class ResourceActor extends Actor {
    /**
     * The resource for this actor.
     */
    @NotNull
    private final Material material;

    /**
     * Creates a new actor with the provided resource.
     *
     * @param material The resource for this actor
     * @param x The initial X position
     * @param y The initial Y position
     * @param level The level this actor belongs to
     */
    public ResourceActor(@NotNull Material material, double x, double y, @NotNull Level level) {
        super(x, y, level);

        this.material = Objects.requireNonNull(material, "resource can't be null");
    }

    /**
     * Creates a new actor with the provided resource.<br>
     * <br>
     * The X and Y positions from the tile position will be randomized within the tile.
     *
     * @param material The resource for this actor
     * @param pos The tile this actor will start in
     * @param level The level this actor belongs to
     */
    public ResourceActor(@NotNull Material material, @NotNull TilePos pos, @NotNull Level level) {
        this(
            material,
            pos.x() + level.random().nextDouble(0.2, 0.8),
            pos.y() + level.random().nextDouble(0.2, 0.8),
            level
        );
    }

    /**
     * Gets the resource this actor contains.
     *
     * @return The resource of this actor
     */
    @NotNull
    public Material resource() {
        return material;
    }
}

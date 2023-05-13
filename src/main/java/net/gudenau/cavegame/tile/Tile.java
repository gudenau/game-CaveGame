package net.gudenau.cavegame.tile;

import net.gudenau.cavegame.material.Material;
import net.gudenau.cavegame.util.Identifier;
import net.gudenau.cavegame.util.Lazy;
import net.gudenau.cavegame.util.Registries;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The base tile class.
 */
public class Tile {
    /**
     * A cost used to represent an impassable tile.
     */
    public static final int IMPASSABLE = -1;

    /**
     * The map color of this tile.
     */
    private final int mapColor;

    /**
     * The cost to traverse across this tile.
     */
    private final int cost;

    /**
     * The registered name of this tile.
     */
    private final Lazy<Identifier> name = Lazy.of(() ->
        Registries.TILE.name(this).orElseThrow(() -> new IllegalStateException("Tile " + this + " was not registered"))
    );

    /**
     * Creates a tile instance.
     *
     * @param cost The cost to navigate over this tile
     * @param mapColor The map color of this tile
     */
    public Tile(int cost, int mapColor) {
        this.cost = cost;
        this.mapColor = mapColor;
    }

    /**
     * Creates an impassable tile instance.
     *
     * @param mapColor The map color of this tile
     */
    public Tile(int mapColor) {
        this(IMPASSABLE, mapColor);
    }

    /**
     * Gets the map color of this tile.
     *
     * @return The map color of this tile
     */
    public final int mapColor() {
        return mapColor;
    }

    /**
     * Checks if this tile can be walked on.
     *
     * @return True if this tile can be walked on, false otherwise
     */
    public final boolean passable() {
        return cost != IMPASSABLE;
    }

    @Override
    public String toString() {
        return name.get().toString();
    }

    /**
     * Gets the resources that this tile drops when mined.
     *
     * @return The dropped resources
     */
    @NotNull
    public List<@NotNull Material> resources() {
        return List.of();
    }

    /**
     * Gets the cost of navigating over this tile.
     *
     * @return The cost of navigating over this tile
     */
    public final int pathingCost() {
        return cost;
    }
}

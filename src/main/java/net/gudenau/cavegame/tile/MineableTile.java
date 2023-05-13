package net.gudenau.cavegame.tile;

import net.gudenau.cavegame.api.HardnessLevel;
import org.jetbrains.annotations.NotNull;

/**
 * A tile that can be mined.
 */
public interface MineableTile {
    /**
     * Determines the amount of time and the level of tool required to mine this tile.
     *
     * @return The hardness level of this tile
     */
    @NotNull HardnessLevel level();

    /**
     * Checks if this tile can actually be mined.
     *
     * @return True if mineable, false otherwise
     */
    default boolean isMineable() {
        return !level().unbreakable();
    }

    /**
     * Gets the hardness of this tile.
     *
     * @return The hardness of this tile
     */
    default int hardness() {
        return level().hardness();
    }

    /**
     * The tile that is left after this tile is mined.
     *
     * @return The remaining tile
     */
    @NotNull
    default Tile remainingTile() {
        return Tiles.RUBBLE;
    }
}

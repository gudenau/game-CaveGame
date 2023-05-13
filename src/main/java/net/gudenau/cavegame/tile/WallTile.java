package net.gudenau.cavegame.tile;

import net.gudenau.cavegame.api.HardnessLevel;
import net.gudenau.cavegame.material.Material;
import net.gudenau.cavegame.material.Materials;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A tile that represents a wall.
 */
public class WallTile extends Tile implements MineableTile {
    /**
     * The hardness level of this wall.
     */
    @NotNull
    private final HardnessLevel level;

    public WallTile(@NotNull HardnessLevel level, int mapColor) {
        super(mapColor);

        this.level = level;
    }

    @Override
    @NotNull
    public HardnessLevel level() {
        return level;
    }

    @Override
    @NotNull
    public List<@NotNull Material> resources() {
        return List.of(Materials.ORE, Materials.ORE);
    }
}

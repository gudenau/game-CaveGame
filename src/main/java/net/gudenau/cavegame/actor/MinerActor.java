package net.gudenau.cavegame.actor;

import net.gudenau.cavegame.level.Level;
import net.gudenau.cavegame.material.Material;
import net.gudenau.cavegame.tile.Tile;
import net.gudenau.cavegame.tile.Tiles;
import net.gudenau.cavegame.tile.WallTile;
import net.gudenau.cavegame.tile.state.StoreRoomState;
import net.gudenau.cavegame.util.TilePos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

/**
 * The primary player controlled actor.
 */
public class MinerActor extends LivingActor {
    /**
     * Creates a new miner instance.
     *
     * @param x The initial X position
     * @param y The initial Y position
     * @param level The level this miner belongs to
     */
    public MinerActor(double x, double y, @NotNull Level level) {
        super(x, y, level);
    }
}

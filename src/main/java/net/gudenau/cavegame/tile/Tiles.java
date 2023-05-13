package net.gudenau.cavegame.tile;

import net.gudenau.cavegame.CaveGame;
import net.gudenau.cavegame.api.HardnessLevel;
import net.gudenau.cavegame.tile.state.StoreRoomState;
import net.gudenau.cavegame.util.Identifier;
import net.gudenau.cavegame.util.Registries;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * All of the default tiles.
 */
public class Tiles {
    public static final Tile BEDROCK = register("bedrock", new WallTile(HardnessLevel.BEDROCK, 0x00_00_00));

    public static final Tile FLOOR = register("floor", new Tile(10, 0xF0_F0_F0));
    public static final Tile RUBBLE = register("rubble", new RubbleTile(0xA0_60_00));

    public static final Tile DIRT_WALL = register("dirt_wall", new WallTile(HardnessLevel.DIRT, 0x80_40_00));
    public static final Tile ROCK_WALL = register("rock_wall", new WallTile(HardnessLevel.ROCK, 0x80_80_80));
    public static final Tile HARD_ROCK_WALL = register("hard_rock_wall", new WallTile(HardnessLevel.HARD_ROCK, 0x40_40_40));

    public static final Tile WATER = register("water", new Tile(0x00_00_A0));
    public static final Tile LAVA = register("lava", new Tile(0xA0_00_00));

    public static final Tile STORE_ROOM = register("store_room", new BuildingTile<>(StoreRoomState::new, 0xFF_00_FF));

    /**
     * Registers a tile and returns it.
     *
     * @param name The name of the tile to register
     * @param tile The tile to register
     * @return The passed tile
     * @param <T> The type of the tile
     */
    @Contract("_, _ -> param2")
    @NotNull
    private static <T extends Tile> T register(@NotNull String name, @NotNull T tile) {
        Registries.TILE.register(new Identifier(CaveGame.NAMESPACE, name), tile);
        return tile;
    }
}

package net.gudenau.cavegame.tile;

import net.gudenau.cavegame.tile.state.TileState;
import net.gudenau.cavegame.tile.state.TileWithState;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A tile that contains a building.
 *
 * @param <T> The state type for the building
 */
public class BuildingTile<T extends TileState> extends Tile implements TileWithState<T> {
    /**
     * The factory for the building state.
     */
    @NotNull
    private final Supplier<@NotNull T> stateFactory;

    /**
     * Creates a new building tile with the provided state factory and map color.
     *
     * @param stateFactory The state factory to use
     * @param mapColor The map color to use
     */
    public BuildingTile(@NotNull Supplier<@NotNull T> stateFactory, int mapColor) {
        super(mapColor);

        this.stateFactory = Objects.requireNonNull(stateFactory, "stateFactory can't be null");
    }

    @NotNull
    @Override
    public T createState() {
        return stateFactory.get();
    }
}

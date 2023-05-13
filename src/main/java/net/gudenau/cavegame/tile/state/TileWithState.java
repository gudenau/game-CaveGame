package net.gudenau.cavegame.tile.state;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Tiles that require state implement this interface in order to provide the state.
 * @param <T> The type of the state class
 */
public interface TileWithState<T extends TileState> {
    /**
     * Creates a new instance of the state for this tile.
     *
     * @return The new state instance
     */
    @Contract("-> new") @NotNull T createState();
}

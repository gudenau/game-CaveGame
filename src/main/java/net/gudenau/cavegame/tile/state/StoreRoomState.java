package net.gudenau.cavegame.tile.state;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.gudenau.cavegame.material.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * The state for a store room tile.
 */
public class StoreRoomState extends TileState {
    /**
     * All stored {@link Material}s in this store room.
     */
    private final Object2IntMap<Material> storage = new Object2IntOpenHashMap<>();

    /**
     * Stores a {@link Material} in this store room.
     *
     * @param material The {@link Material} to store
     * @param amount The amount of the {@link Material}
     */
    public void storeResource(@NotNull Material material, int amount) {
        Objects.requireNonNull(material, "resource can't be null");
        storage.computeInt(material, (key, existing) -> (existing == null ? 0 : existing) + amount);
    }

    /**
     * Gets the amount of a {@link Material} stored in this store room.
     *
     * @param material The {@link Material} to query
     * @return The amount of the {@link Material}
     */
    public int storedResource(@NotNull Material material) {
        Objects.requireNonNull(material, "resource can't be null");
        return storage.getInt(material);
    }
}

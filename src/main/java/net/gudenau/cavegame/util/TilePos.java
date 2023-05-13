package net.gudenau.cavegame.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

/**
 * An immutable 2D position.
 *
 * @param x The X position
 * @param y The Y position
 */
public record TilePos(int x, int y) {
    /**
     * The mask used to extract the X value.
     */
    private static final long X_MASK = 0x00000000FFFFFFFFL;

    /**
     * The mask used to extract the Y value.
     */
    private static final long Y_MASK = 0xFFFFFFFF00000000L;

    /**
     * The value used to pack and extract the X value.
     */
    private static final int X_SHIFT = 0;

    /**
     * The value used to pack and extract the Y value.
     */
    private static final int Y_SHIFT = 32;

    /**
     * Unpacks the long representation of a position.
     *
     * @param pos The long to unpack
     */
    public TilePos(long pos) {
        this(
            (int) ((pos & X_MASK) >>> X_SHIFT),
            (int) ((pos & Y_MASK) >>> Y_SHIFT)
        );
    }

    /**
     * Creates a new position from the floor of a pair of doubles.
     *
     * @param x The X value
     * @param y The Y value
     */
    public TilePos(double x, double y) {
        this((int) Math.floor(x), (int) Math.floor(y));
    }

    /**
     * Creates an iterator that iterates over the provided 2D area.
     *
     * @param startX The starting X position
     * @param startY The starting Y position
     * @param endX The ending X position
     * @param endY The ending Y position
     * @return The new iterator
     */
    @Contract("_, _, _, _ -> new")
    @NotNull
    public static Iterator<@NotNull TilePos> iterator(int startX, int startY, int endX, int endY) {
        return new Iterator<>() {
            int x = startX;
            int y = startY;

            @Override
            public boolean hasNext() {
                return y < endY;
            }

            @Override
            public TilePos next() {
                var pos = new TilePos(x++, y);

                if(x >= endX) {
                    x = startX;
                    y++;
                }

                return pos;
            }
        };
    }

    /**
     * Packs this position into a long.
     *
     * @return The long representation of this position
     */
    public long asLong() {
        return (((long) x) << X_SHIFT) | (((long) y) << Y_SHIFT);
    }

    /**
     * Gets a list of neighbors.
     *
     * @return A list of neighbors
     */
    public List<TilePos> neighbors() {
        return List.of(
            new TilePos(x - 1, y),
            new TilePos(x + 1, y),
            new TilePos(x, y - 1),
            new TilePos(x, y + 1)
        );
    }

    /**
     * Gets the rounded distance to another position.
     *
     * @param pos The position to get the distance to
     * @return The distance
     */
    public int distanceTo(@NotNull TilePos pos) {
        return (int) Math.sqrt(squaredDistanceTo(pos));
    }

    /**
     * Gets the squared distance to another position.
     *
     * @param pos The position to get the distance to
     * @return The squared distance
     */
    public int squaredDistanceTo(@NotNull TilePos pos) {
        return (int) (Math.pow(x - pos.x, 2) + Math.pow(y - pos.y, 2));
    }

    /**
     * Checks if this position is adjacent to another position.
     *
     * @param neighbor The position to check
     * @return True if the position to check, false otherwise
     */
    public boolean isAdjacentTo(@NotNull TilePos neighbor) {
        return Math.abs(x - neighbor.x) + Math.abs(y - neighbor.y) == 1;
    }
}

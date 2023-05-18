package net.gudenau.cavegame.actor;

import net.gudenau.cavegame.level.Level;
import net.gudenau.cavegame.util.TilePos;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * An actor that can exist in a level. Used for anything that is not based on tiles, like resources, player characters
 * and monsters.
 */
public abstract class Actor {
    /**
     * The level instance that this actor exists in.
     */
    protected final Level level;

    /**
     * The X position of this actor.
     */
    protected double x;

    /**
     * The Y position of this actor.
     */
    protected double y;

    /**
     * A flag that marks this actor for removal at the end of a tick.
     */
    private boolean needsRemoval = false;

    /**
     * Creates a new actor instance at the provided position with the provided level instance.
     *
     * @param x The initial X position
     * @param y The initial Y position
     * @param level The level of this actor
     */
    public Actor(double x, double y, @NotNull Level level) {
        this.level = Objects.requireNonNull(level, "level can't be null");
        this.x = x;
        this.y = y;
    }

    /**
     * Allows the actor to do work.
     */
    public void tick() {}

    /**
     * Gets the X position of this actor.
     *
     * @return The X position of this actor
     */
    public final double x() {
        return x;
    }

    /**
     * Gets the Y position of this actor.
     *
     * @return The Y position of this actor
     */
    public final double y() {
        return y;
    }

    /**
     * The {@link TilePos} of this actor.<br>
     * <br>
     * Equivalent to:
     * {@snippet :
     *     new TilePos(actor.x(), actor.y())
     * }
     *
     * @return The {@link TilePos} of this actor
     */
    @NotNull
    public final TilePos tilePos() {
        return new TilePos(x, y);
    }

    /**
     * Checks if this actor needs to be removed at the end of a tick.
     *
     * @return True if the actor needs to be removed, false otherwise
     */
    public boolean needsRemoval() {
        return needsRemoval;
    }

    /**
     * Marks this actor for removal at the end of a tick.
     */
    public void remove() {
        needsRemoval = true;
    }

    @NotNull
    public Level level() {
        return level;
    }

    public void onSpawned() {
    }
}

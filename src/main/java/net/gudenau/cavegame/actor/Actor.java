package net.gudenau.cavegame.actor;

import net.gudenau.cavegame.level.Level;
import net.gudenau.cavegame.tile.Tile;
import net.gudenau.cavegame.util.TilePos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

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
    private double x;

    /**
     * The Y position of this actor.
     */
    private double y;

    protected double facing = 0;

    /**
     * A flag that marks this actor for removal at the end of a tick.
     */
    private boolean needsRemoval = false;

    @Nullable
    private LivingActor holder;

    @Nullable
    private TilePos tilePos;

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
        if(tilePos != null) {
            return tilePos;
        }
        return tilePos = new TilePos(x, y);
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

    @NotNull
    protected Optional<LivingActor> holder() {
        return Optional.ofNullable(holder);
    }

    protected void holder(@Nullable LivingActor actor) {
        holder = actor;
    }

    public boolean isHeld() {
        return holder != null;
    }

    protected void pos(double x, double y) {
        this.x = x;
        this.y = y;
        tilePos = null;
    }

    public double posX() {
        return x;
    }

    public double posY() {
        return y;
    }

    public void removed() {
        if(holder != null) {
            holder.drop(this);
            holder = null;
        }
    }

    public boolean isAdjacentTo(@NotNull Tile tile) {
        return tilePos().neighbors().stream()
            .map(level::tile)
            .anyMatch((neighbor) -> neighbor == tile);
    }

    public Optional<TilePos> findAdjacentTile(@NotNull Tile tile) {
        return tilePos().neighbors().stream()
            .filter((pos) -> level.tile(pos) == tile)
            .findAny();
    }
}

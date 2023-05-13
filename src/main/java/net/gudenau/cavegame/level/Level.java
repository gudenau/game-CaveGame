package net.gudenau.cavegame.level;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.gudenau.cavegame.actor.Actor;
import net.gudenau.cavegame.actor.LivingActor;
import net.gudenau.cavegame.actor.ResourceActor;
import net.gudenau.cavegame.tile.MineableTile;
import net.gudenau.cavegame.tile.Tile;
import net.gudenau.cavegame.tile.Tiles;
import net.gudenau.cavegame.tile.state.TileState;
import net.gudenau.cavegame.tile.state.TileWithState;
import net.gudenau.cavegame.util.LockedRandom;
import net.gudenau.cavegame.util.TilePos;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Contains all of the tiles and actors of a game.
 */
public final class Level {
    /**
     * The width (in tiles) of this level.
     */
    private final int width;

    /**
     * The height (in tiles) of this level.
     */
    private final int height;

    /**
     * The tiles of this level.
     */
    @NotNull
    private final Tile @NotNull [] tiles;

    /**
     * The actors in this level.
     */
    @NotNull
    private final Set<@NotNull Actor> actors = new HashSet<>();

    /**
     * The actors that need to be added.
     */
    @NotNull
    private final Set<@NotNull Actor> pendingActors = new HashSet<>();

    /**
     * Any extra state the tiles need.
     */
    @NotNull
    private final Long2ObjectMap<TileState> tileState = new Long2ObjectOpenHashMap<>();

    /**
     * The mining progress of the tiles.
     */
    @NotNull
    private final Long2IntMap miningProgress = new Long2IntOpenHashMap();

    /**
     * A random number generator.
     */
    @NotNull
    private final RandomGenerator random = new LockedRandom();

    /**
     * Creates a new level with the provided size that is filled with {@link Tiles#BEDROCK}.
     *
     * @param width The width of the level
     * @param height The height of the level
     */
    public Level(int width, int height) {
        this.width = width;
        this.height = height;

        tiles = new Tile[width * height];
        Arrays.fill(tiles, Tiles.BEDROCK);
    }

    /**
     * Gets the width of this level.
     *
     * @return the width of this level
     */
    public int width() {
        return width;
    }

    /**
     * Gets the height of this level.
     *
     * @return the height of this level
     */
    public int height() {
        return height;
    }

    /**
     * Gets the {@link Tile} at the provided location, returns {@link Tiles#BEDROCK} for invalid coordinates.
     *
     * @param pos The position of the {@link Tile} to get
     * @return The {@link Tile} at the provided position
     */
    @NotNull
    public Tile tile(@NotNull TilePos pos) {
        return inBounds(pos) ? tiles[pos.x() + pos.y() * width] : Tiles.BEDROCK;
    }

    /**
     * Gets the {@link TileState} at the provided position.
     *
     * @param pos The position of the {@link TileState} to get
     * @return The {@link TileState} or empty if not present
     */
    @NotNull
    public Optional<TileState> tileState(@NotNull TilePos pos) {
        return Optional.ofNullable(tileState.get(pos.asLong()));
    }

    /**
     * Get the {@link TileState} at the provided position or empty if it doesn't match the provided type.
     *
     * @param pos The position of the {@link TileState} to get
     * @param type The class of the {@link TileState}
     * @return The {@link TileState} or empty if not present or not of the provided type
     * @param <T> The generic type of the {@link TileState}
     */
    @SuppressWarnings("unchecked")
    @NotNull
    public <T extends TileState> Optional<T> tileState(@NotNull TilePos pos, @NotNull Class<T> type) {
        return tileState(pos).map((state) -> type.isInstance(state) ? (T) state : null);
    }

    /**
     * Sets the {@link Tile} at a given position.<br>
     * Side effects include:
     * <ul>
     *     <li>Resetting mining progress</li>
     *     <li>Deleting tile state</li>
     * </ul>
     * This does nothing for an out-of-bounds position.
     *
     * @param pos The position to modify
     * @param tile The {@link Tile} to set
     */
    public void tile(@NotNull TilePos pos, @NotNull Tile tile) {
        var x = pos.x();
        var y = pos.y();

        if (!inBounds(pos)) {
            return;
        }

        int index = x + y * width;
        var existing = tiles[index];
        miningProgress.remove(pos.asLong());
        if(existing == tile) {
            return;
        }

        tiles[index] = tile;

        if (tile instanceof TileWithState<?> withState) {
            tileState.put(pos.asLong(), withState.createState());
        } else {
            tileState.remove(pos.asLong());
        }
    }

    /**
     * Ticks everything in this level that requires ticking.
     */
    public void tick() {
        actors.forEach(Actor::tick);
        actors.addAll(pendingActors);
        pendingActors.clear();
        actors.stream()
            .filter(Actor::needsRemoval)
            .toList()
            .forEach(actors::remove);
    }

    /**
     * Spawns a new {@link Actor} in this level.
     * @param actor The actor to spawn
     */
    public void spawn(@NotNull Actor actor) {
        Objects.requireNonNull(actor, "actor can't be null");
        pendingActors.add(actor);
    }

    /**
     * Gets all {@link Actor}s currently in this level.
     *
     * @return An immutable {@link Set<Actor>} of {@link Actor}s
     */
    public Set<Actor> actors() {
        return Stream.of(actors, pendingActors)
            .flatMap(Collection::stream)
            .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Finds a specific {@link Tile} in this level.
     *
     * @param tile The {@link Tile} to look for
     * @return The {@link TilePos} or empty if the {@link Tile} was not found
     */
    public Optional<TilePos> findTile(@NotNull Tile tile) {
        Objects.requireNonNull(tile, "tile can't be null");

        //TODO Optimize, this is garbage.
        for (int i = 0, length = tiles.length; i < length; i++) {
            if(tiles[i] == tile) {
                return Optional.of(new TilePos(i % width, i / height));
            }
        }
        return Optional.empty();
    }

    /**
     * Finds the closed instance of a specific {@link Tile}.
     *
     * @param position The position to start searching from
     * @param filter The {@link Tile} {@link Predicate}
     * @return The {@link TilePos} or empty if a {@link Tile} was not found
     */
    public Optional<TilePos> findNearestTile(TilePos position, Predicate<Tile> filter) {
        //TODO Optimize this?

        var visited = new HashSet<TilePos>();
        visited.add(position);

        Queue<TilePos> toVisit = new LinkedList<>(position.neighbors());

        while(!toVisit.isEmpty()) {
            var pos = toVisit.remove();
            visited.add(pos);
            var tile = tile(pos);

            if(filter.test(tile)) {
                return Optional.of(pos);
            }

            for (var neighbor : pos.neighbors()) {
                if(inBounds(neighbor) && !visited.contains(neighbor)) {
                    toVisit.add(neighbor);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Checks if the provided {@link TilePos} is in bounds of this level.
     *
     * @param pos The {@link TilePos} to check
     * @return True if the {@link TilePos} is in bounds, false if otherwise
     */
    private boolean inBounds(@NotNull TilePos pos) {
        var x = pos.x();
        var y = pos.y();
        return x >= 0 && y >= 0 && x < width && y < height;
    }


    /**
     * Updates the dig progress of a mineable tile, does nothing if the tile is not mineable. If the dig progress is
     * greater than the hardness of the tile it will be replaced with the remainder tile.
     *
     * @param pos The position of the tile to dig
     * @param amount The amount of progress to make
     */
    public void digTile(TilePos pos, int amount) {
        var tile = tile(pos);
        if(!(tile instanceof MineableTile mineable)) {
            return;
        }

        var progress = miningProgress.compute(pos.asLong(), (key, prog) -> (prog == null ? 0 : prog) + amount);
        if(progress < mineable.hardness()) {
            return;
        }

        tile.resources().stream()
            .map((resource) -> new ResourceActor(resource, pos, this))
            .forEach(this::spawn);
        tile(pos, mineable.remainingTile());
    }

    /**
     * The random instance for this level.
     *
     * @return The random instance for this level
     */
    @NotNull
    public RandomGenerator random() {
        return random;
    }
}

package net.gudenau.cavegame.level;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.gudenau.cavegame.actor.LivingActor;
import net.gudenau.cavegame.tile.Tile;
import net.gudenau.cavegame.util.MathUtils;
import net.gudenau.cavegame.util.SharedLock;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class Pathfinder {
    @NotNull
    private final SharedLock lock = new SharedLock();
    @NotNull
    private final Level level;
    @NotNull
    private final Map<Class<? extends LivingActor>, Map<CacheKey, CacheEntry>> cache = new Object2ObjectOpenHashMap<>();

    public void tileModified(@NotNull TilePos pos, @NotNull Tile existing, @NotNull Tile current) {
        if(existing.passable() == current.passable()) {
            return;
        }

        lock.write(() -> {
            cache.values().forEach((map) -> {
                    map.entrySet().stream()
                        .filter((entry) -> {
                            var cacheEntry = entry.getValue();
                            return cacheEntry.result.isEmpty() || cacheEntry.result.get().contains(pos);
                        })
                        .collect(Collectors.toUnmodifiableSet())
                        .forEach((entry) -> map.remove(entry.getKey()));
                });
        });
    }

    private record CacheKey(TilePos start, TilePos end) {}

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static final class CacheEntry {
        @NotNull
        private final Optional<PathResult> result;
        private int age = 0;

        private CacheEntry(@NotNull Optional<PathResult> result) {
            this.result = result;
        }

        public Optional<PathResult> get() {
            age = 0;
            return result;
        }
    }

    Pathfinder(@NotNull Level level) {
        this.level = level;
    }

    public void purge() {
        lock.write(() -> cache.values().forEach(Map::clear));
    }

    //TODO
    public void purgeOld() {
        /*
        lock.write(() -> cache.values().forEach((map) -> {
            map.entrySet().stream()
                .filter((entry) -> entry.getValue().age++ >= 20)
                .collect(Collectors.toUnmodifiableSet())
                .forEach(map::remove);
        }));
         */
    }

    /**
     * The results of a pathfinding operation.
     *
     * @param path The list of positions that represent the path to traverse
     * @param cost The cost of the path
     */
    public record PathResult(
        @NotNull List<TilePos> path,
        long cost
    ){
        public PathResult(List<TilePos> path, long cost) {
            Objects.requireNonNull(path, "path can't be null");

            this.path = List.copyOf(path);
            this.cost = cost;
        }

        public boolean contains(TilePos pos) {
            return path.contains(pos);
        }
    }

    /**
     * Estimates the cost to navigate to a tile.
     *
     * @param goal The position of the goal
     * @param start The starting position
     * @return The estimated cost of navigating to a tile
     */
    private long estimateCost(@NotNull TilePos goal, @NotNull TilePos start) {
        return goal.distanceTo(start);
    }

    /**
     * Computes the cost to navigate to a neighboring tile.
     *
     * @param start The starting position
     * @param neighbor The neighbor position
     * @return The calculated cost of navigating to the neighbor
     */
    private long costToNeighbor(@NotNull TilePos start, @NotNull TilePos neighbor) {
        if(!start.isAdjacentTo(neighbor)) {
            throw new IllegalArgumentException(start + " was not adjacent to " + neighbor);
        }

        var tile = level.tile(neighbor);
        if (tile.passable()) {
            return tile.pathingCost();
        } else {
            return Long.MAX_VALUE;
        }
    }

    /**
     * Calculates a path from the current position to the goal, uses A* and is based off of the Wikipedia pseudo-code.
     *
     * @param goal The navigation target
     * @return The {@link PathResult} if a path was found, empty otherwise
     */
    public Optional<PathResult> calculatePath(@NotNull LivingActor actor, @NotNull TilePos goal) {
        var start = actor.tilePos();
        var key = new CacheKey(start, goal);
        var cachedResult = lock.read(() -> cache.getOrDefault(actor.getClass(), Map.of()).get(key));
        if(cachedResult != null) {
            return cachedResult.get();
        }

        //System.out.println("Attempting to pathfind to " + goal + " from " + start);
        if(goal.equals(start)) {
            return Optional.of(new PathResult(List.of(), 0));
        }

        List<TilePos> openSet = new LinkedList<>();
        openSet.add(start);

        Map<TilePos, TilePos> cameFrom = new HashMap<>();

        Object2LongMap<TilePos> gScore = new Object2LongOpenHashMap<>();
        gScore.defaultReturnValue(Long.MAX_VALUE);
        gScore.put(start, 0);

        Object2LongMap<TilePos> fScore = new Object2LongOpenHashMap<>();
        fScore.defaultReturnValue(Long.MAX_VALUE);
        fScore.put(start, estimateCost(goal, start));

        while(!openSet.isEmpty()) {
            var current = openSet.stream()
                .min(Comparator.comparingLong(fScore::getLong))
                .get();

            if(current.equals(goal)) {
                List<TilePos> path = new ArrayList<>();
                long cost = 0;
                path.add(current);
                while(cameFrom.containsKey(current)) {
                    var next = cameFrom.get(current);
                    cost += costToNeighbor(next, current);
                    path.add(next);
                    current = next;
                }
                Collections.reverse(path);
                path.remove(0);

                var result = Optional.of(new PathResult(path, cost));
                lock.write(() -> cache.computeIfAbsent(actor.getClass(), (k) -> new Object2ObjectOpenHashMap<>()).put(key, new CacheEntry(result)));
                return result;
            }

            openSet.remove(current);

            for(var neighbor : current.neighbors()) {
                var tenativeGScore = MathUtils.saturatingAdd(gScore.getLong(current), costToNeighbor(current, neighbor));
                if(tenativeGScore < gScore.getLong(neighbor)) {
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tenativeGScore);
                    fScore.put(neighbor, tenativeGScore + estimateCost(goal, neighbor));
                    openSet.add(neighbor);
                }
            }
        }

        lock.write(() -> cache.computeIfAbsent(actor.getClass(), (k) -> new Object2ObjectOpenHashMap<>()).put(key, new CacheEntry(Optional.empty())));
        return Optional.empty();
    }

    /**
     * Calculates a path from the current position to all the provided goals, returns the cheapest successful path.
     *
     * @param goals The navigation targets
     * @return The {@link PathResult} of the cheapest found path, empty if no paths where found
     */
    public Optional<PathResult> calculateCheapestPath(@NotNull LivingActor actor, List<TilePos> goals) {
        return goals.stream()
            .map((goal) -> calculatePath(actor, goal))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .min(Comparator.comparingLong(PathResult::cost));
    }

    /**
     * Calculates the cost to the cheapest side of a tile or the tile itself if it's passable.
     *
     * @param goal The tile to navigate to
     * @return True if pathfinding succeeded
     */
    public Optional<PathResult> calculateCheapestPathToSide(@NotNull LivingActor actor, @NotNull TilePos goal) {
        if(level.tile(goal).passable()) {
            return calculatePath(actor, goal);
        } else {
            return calculateCheapestPath(actor, goal.neighbors().stream().filter((pos) -> level.tile(pos).passable()).toList());
        }
    }
}

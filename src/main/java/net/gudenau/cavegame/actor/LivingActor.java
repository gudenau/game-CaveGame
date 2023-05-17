package net.gudenau.cavegame.actor;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.gudenau.cavegame.level.Level;
import net.gudenau.cavegame.util.MathUtils;
import net.gudenau.cavegame.util.TilePos;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Any "living" actor, defined as an actor that can navigate and has health.
 */
public class LivingActor extends Actor {
    /**
     * The health of this actor.
     */
    protected double health = 10;

    /**
     * The nodes used for pathfinding. The actor will follow these in order.
     */
    protected Queue<TilePos> nodes = new LinkedList<>();

    /**
     * Creates a new living actor.
     *
     * @param x The initial X position
     * @param y The initial Y position
     * @param level The level the actor is in
     */
    public LivingActor(double x, double y, @NotNull Level level) {
        super(x, y, level);
    }

    @Override
    public void tick() {
        super.tick();

        // Navigation
        if(!nodes.isEmpty()) {
            //TODO Make this smoother
            var node = nodes.remove();
            this.x = node.x() + 0.5;
            this.y = node.y() + 0.5;
        }
    }

    /**
     * Checks if the health of this actor is greater than 0.
     *
     * @return True if the actor has health, false otherwise
     */
    public final boolean isAlive() {
        return health > 0;
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
     * The results of a pathfinding operation.
     *
     * @param path The list of positions that represent the path to traverse
     * @param cost The cost of the path
     */
    protected record PathResult(
        @NotNull List<TilePos> path,
        long cost
    ){
        public PathResult(List<TilePos> path, long cost) {
            Objects.requireNonNull(path, "path can't be null");

            this.path = List.copyOf(path);
            this.cost = cost;
        }
    }

    /**
     * Calculates a path from the current position to the goal, uses A* and is based off of the Wikipedia pseudo-code.
     *
     * @param goal The navigation target
     * @return The {@link PathResult} if a path was found, empty otherwise
     */
    protected Optional<PathResult> calculatePath(@NotNull TilePos goal) {
        var start = tilePos();
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

                return Optional.of(new PathResult(path, cost));
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

        return Optional.empty();
    }

    /**
     * Calculates a path from the current position to all the provided goals, returns the cheapest successful path.
     *
     * @param goals The navigation targets
     * @return The {@link PathResult} of the cheapest found path, empty if no paths where found
     */
    protected Optional<PathResult> calculateCheapestPath(List<TilePos> goals) {
        return goals.stream()
            .map(this::calculatePath)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .min(Comparator.comparingLong(PathResult::cost));
    }

    /**
     * Starts pathfinding to a goal position.
     *
     * @param goal The target to pathfind to
     * @return true if a path was found, false otherwise
     */
    public boolean navigate(@NotNull TilePos goal) {
        var result = calculatePath(goal);
        if(result.isPresent()) {
            nodes.clear();
            nodes.addAll(result.get().path());
        }
        return result.isPresent();
    }

    /**
     * Starts pathfinding to the cheapest goal position.
     *
     * @param goals The targets to pathfind to
     * @return true if a path was found, false otherwise
     */
    public boolean navigateToCheapest(@NotNull List<TilePos> goals) {
        var result = calculateCheapestPath(goals);
        if(result.isPresent()) {
            nodes.clear();
            nodes.addAll(result.get().path());
        }
        return result.isPresent();
    }

    /**
     * Navigates to the cheapest side of a tile or the tile itself if it's passable.
     *
     * @param goal The tile to navigate to
     * @return True if pathfinding succeeded
     */
    public boolean navigateToSide(TilePos goal) {
        if(level.tile(goal).passable()) {
            return navigate(goal);
        } else {
            return navigateToCheapest(goal.neighbors().stream().filter((pos) -> level.tile(pos).passable()).toList());
        }
    }

    /**
     * Checks if this actor has a path that it is following.
     *
     * @return true if following a path, false otherwise
     */
    protected boolean hasNavTarget() {
        return !nodes.isEmpty();
    }

    @Override
    public boolean needsRemoval() {
        return super.needsRemoval() || !isAlive();
    }
}

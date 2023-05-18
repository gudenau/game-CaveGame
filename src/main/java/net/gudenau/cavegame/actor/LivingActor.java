package net.gudenau.cavegame.actor;

import net.gudenau.cavegame.ai.Job;
import net.gudenau.cavegame.level.Level;
import net.gudenau.cavegame.level.Pathfinder;
import net.gudenau.cavegame.util.TilePos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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

    @Nullable
    private Job job = null;

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
        } else if(job == null) {
            job = level.jobManager().findJob(this).orElse(null);
            if(job != null) {
                job.start(this);
            }
        } else {
            job.tick(this);
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

    public void removeJob(boolean failed) {
        if(failed) {
            level.jobManager().enqueueJob(job);
        }
        job = null;
    }

    /**
     * Starts pathfinding to a goal position.
     *
     * @param goal The target to pathfind to
     * @return true if a path was found, false otherwise
     */
    public boolean navigate(@NotNull TilePos goal) {
        var result = level.pathfinder().calculatePath(this, goal);
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
        var result = level.pathfinder().calculateCheapestPath(this, goals);
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

    void navigate(@NotNull Pathfinder.PathResult result) {
        nodes.clear();
        nodes.addAll(result.path());
    }
}

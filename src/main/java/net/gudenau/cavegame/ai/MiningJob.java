package net.gudenau.cavegame.ai;

import net.gudenau.cavegame.actor.LivingActor;
import net.gudenau.cavegame.level.Pathfinder;
import net.gudenau.cavegame.tile.Tile;
import net.gudenau.cavegame.util.TilePos;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record MiningJob(
    @NotNull Tile tile,
    @NotNull TilePos position
) implements Job {
    public MiningJob {
        Objects.requireNonNull(tile, "tile can't be null");
        Objects.requireNonNull(position, "position can't be null");
    }

    public MiningJob(@NotNull Object state) {
        this(null, null);
    }

    @Override
    public long estimateCost(@NotNull LivingActor actor) {
        var result = actor.level().pathfinder().calculateCheapestPathToSide(actor, position);
        return result.map(Pathfinder.PathResult::cost).orElse(-1L);
    }

    @Override
    public void start(LivingActor actor) {
        actor.navigateToSide(position);
    }

    @Override
    public void tick(LivingActor actor) {
        if(actor.tilePos().isAdjacentTo(position)) {
            var level = actor.level();
            level.digTile(position, 1);
            if(level.tile(position) != tile) {
                actor.removeJob(false);
            }
        } else if(!actor.navigateToSide(position)) {
            actor.removeJob(true);
            System.out.println("Failed to navigate in job " + this);
        }
    }
}

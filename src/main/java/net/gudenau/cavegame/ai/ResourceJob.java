package net.gudenau.cavegame.ai;

import net.gudenau.cavegame.actor.LivingActor;
import net.gudenau.cavegame.actor.ResourceActor;
import net.gudenau.cavegame.level.Pathfinder;
import net.gudenau.cavegame.tile.Tiles;
import org.jetbrains.annotations.NotNull;

public record ResourceJob(
    @NotNull ResourceActor resource
) implements Job {
    public ResourceJob(Object state) {
        this(null);
    }

    @Override
    public long estimateCost(@NotNull LivingActor actor) {
        var cost = actor.level().pathfinder().calculatePath(actor, resource.tilePos());
        return cost.map(Pathfinder.PathResult::cost).orElse(-1L);
    }

    @Override
    public void start(LivingActor actor) {
        actor.navigate(resource.tilePos());
    }

    @Override
    public void tick(LivingActor actor) {
        var position = resource.tilePos();
        if(position.equals(actor.tilePos()) && !resource.needsRemoval()) {
            resource.remove();
            actor.level().findNearestTile(position, Tiles.STORE_ROOM)
                .ifPresent(actor::navigateToSide);
        } else {
            actor.removeJob(!resource.needsRemoval());
        }
    }
}

package net.gudenau.cavegame.ai;

import net.gudenau.cavegame.actor.LivingActor;
import net.gudenau.cavegame.actor.ResourceActor;
import net.gudenau.cavegame.level.Pathfinder;
import net.gudenau.cavegame.tile.Tiles;
import net.gudenau.cavegame.tile.state.StoreRoomState;
import org.jetbrains.annotations.NotNull;

public record ResourceJob(
    @NotNull ResourceActor resource
) implements Job {
    public ResourceJob(Object state) {
        this(null);
    }

    @Override
    public long estimateCost(@NotNull LivingActor actor) {
        if(actor.isHeld()) {
            return -1;
        }
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
        var held = actor.heldActor().orElse(null);
        if(held == resource) {
            var storeRoomPos = actor.findAdjacentTile(Tiles.STORE_ROOM).orElse(null);
            if(storeRoomPos != null) {
                actor.level().tileState(storeRoomPos, StoreRoomState.class)
                    .ifPresent((state) -> state.storeResource(resource.resource(), 1));
                resource.remove();
                actor.removeJob(!resource.needsRemoval());
            }
        }else if(position.equals(actor.tilePos()) && !resource.needsRemoval()) {
            if(!actor.pickup(resource)) {
                actor.removeJob(true);
                return;
            }
            actor.level().findNearestTile(position, Tiles.STORE_ROOM)
                .ifPresent(actor::navigateToSide);
        }
    }
}

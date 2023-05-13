package net.gudenau.cavegame.actor;

import net.gudenau.cavegame.level.Level;
import net.gudenau.cavegame.material.Material;
import net.gudenau.cavegame.tile.Tile;
import net.gudenau.cavegame.tile.Tiles;
import net.gudenau.cavegame.tile.WallTile;
import net.gudenau.cavegame.tile.state.StoreRoomState;
import net.gudenau.cavegame.util.TilePos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

/**
 * The primary player controlled actor.
 */
public class MinerActor extends LivingActor {
    /**
     * The resource currently held by this miner, if any.
     */
    @Nullable
    private Material heldMaterial;

    @Nullable
    private TilePos miningTarget;

    @Nullable
    private Tile miningTile;

    /**
     * Creates a new miner instance.
     *
     * @param x The initial X position
     * @param y The initial Y position
     * @param level The level this miner belongs to
     */
    public MinerActor(double x, double y, @NotNull Level level) {
        super(x, y, level);
    }

    @Override
    public void tick() {
        super.tick();

        if(hasNavTarget()) {
            return;
        }

        var pos = tilePos();
        if(heldMaterial != null) {
            var storeRoomPos = pos.neighbors().stream()
                .filter((neighbor) -> level.tile(neighbor) == Tiles.STORE_ROOM)
                .findAny()
                .orElse(null);
            if(storeRoomPos != null) {
                var storeRoom = level.tileState(storeRoomPos, StoreRoomState.class)
                    .orElseThrow(() -> new IllegalStateException("Store room did not have a state?"));
                storeRoom.storeResource(heldMaterial, 1);
                heldMaterial = null;
            } else {
                level.findTile(Tiles.STORE_ROOM).ifPresent((goal) -> {
                    if(!navigateToSide(goal)) {
                        health = 0;
                    }
                });
            }
        } else if(miningTarget != null) {
            level.digTile(miningTarget, 1);
            if(level.tile(miningTarget) != miningTile) {
                miningTarget = null;
                miningTile = null;
            }
        } else {
            var resources = level.actors().stream()
                .filter(ResourceActor.class::isInstance)
                .map(Actor::tilePos)
                .min(Comparator.comparingInt(pos::squaredDistanceTo));
            if(resources.isPresent()) {
                var resourcePos = resources.get();
                if(resourcePos.equals(pos)) {
                    var optionalResource = level.actors().stream()
                        .filter(ResourceActor.class::isInstance)
                        .map(ResourceActor.class::cast)
                        .filter((actor) -> !actor.needsRemoval())
                        .filter((actor) -> actor.tilePos().equals(resourcePos))
                        .findAny();
                    if(optionalResource.isEmpty()) {
                        return;
                    }
                    var resource = optionalResource.get();

                    heldMaterial = resource.resource();
                    resource.remove();
                } else {
                    navigate(resourcePos);
                }
            } else {
                var wallPos = tilePos().neighbors().stream()
                    .filter((candidate) -> level.tile(candidate) instanceof WallTile wall && wall.isMineable())
                    .findFirst();

                if (wallPos.isPresent()) {
                    miningTarget = wallPos.get();
                    miningTile = level.tile(miningTarget);
                } else {
                    level.findNearestTile(pos, (tile) -> tile instanceof WallTile wall && wall.isMineable())
                        .ifPresentOrElse(
                            (goal) -> {
                                if (!navigateToCheapest(goal.neighbors().stream().filter((targetPos) -> level.tile(targetPos).passable()).toList())) {
                                    health = 0;
                                }
                            },
                            () -> health = 0
                        );
                }
            }
        }
    }
}

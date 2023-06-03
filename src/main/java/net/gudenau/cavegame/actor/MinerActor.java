package net.gudenau.cavegame.actor;

import net.gudenau.cavegame.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * The primary player controlled actor.
 */
public class MinerActor extends LivingActor {
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
}

package net.gudenau.cavegame.ai;

import net.gudenau.cavegame.actor.LivingActor;
import org.jetbrains.annotations.NotNull;

public interface Job {
    long estimateCost(@NotNull LivingActor actor);

    void start(LivingActor actor);

    void tick(LivingActor actor);
}

package net.gudenau.cavegame.ai;

import it.unimi.dsi.fastutil.Function;
import net.gudenau.cavegame.CaveGame;
import net.gudenau.cavegame.util.Identifier;
import net.gudenau.cavegame.util.Registries;
import org.jetbrains.annotations.NotNull;

public final class JobTypes {
    public static final JobType<MiningJob> MINING = register("mining", MiningJob::new);
    public static final JobType<ResourceJob> RESOURCE = register("resource", ResourceJob::new);

    private static <T extends Job> JobType<T> register(@NotNull String name, @NotNull Function<Object, T> factory) {
        var type = new JobType<T>(factory);
        Registries.JOB_TYPE.register(new Identifier(CaveGame.NAMESPACE, name), type);
        return type;
    }
}

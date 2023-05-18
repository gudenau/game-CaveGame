package net.gudenau.cavegame.ai;

import it.unimi.dsi.fastutil.Function;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.gudenau.cavegame.util.Identifier;
import net.gudenau.cavegame.util.SharedLock;
import net.gudenau.cavegame.util.Treachery;
import net.jodah.typetools.TypeResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

//TODO Serialization
public final class JobType<T extends Job> {
    private static final SharedLock LOCK = new SharedLock();
    private static final Map<Class<?>, JobType<?>> TYPE_MAP = new Object2ObjectOpenHashMap<>();

    private final Function<Object, T> factory;

    @SuppressWarnings("unchecked")
    public JobType(@NotNull Function<Object, T> factory) {
        this.factory = factory;

        var results = TypeResolver.resolveRawArguments(Function.class, factory.getClass());
        var jobType = (Class<T>) results[1];

        LOCK.write(() -> TYPE_MAP.put(jobType, this));
    }

    @SuppressWarnings("unchecked")
    public static <T extends Job> JobType<T> from(T job) {
        var type = LOCK.read(() -> TYPE_MAP.get(job.getClass()));
        if(type == null) {
            throw new IllegalStateException(Treachery.longClassName(job.getClass()) + " was not registered");
        }
        return (JobType<T>) type;
    }
}

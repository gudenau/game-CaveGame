package net.gudenau.cavegame.util.collection;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collector;

public final class FastCollectors {
    private FastCollectors() {
        throw new AssertionError();
    }

    @NotNull
    public static <I, V> Collector<I, Int2ObjectMap<V>, Int2ObjectMap<V>> toInt2ObjectMap(
        @NotNull ToIntFunction<I> key,
        @NotNull Function<I, V> value
    ) {
        return Collector.of(
            Int2ObjectOpenHashMap::new,
            (map, element) -> map.put(key.applyAsInt(element), value.apply(element)),
            (a, b) -> {
                var q = new Int2ObjectOpenHashMap<>(a);
                q.putAll(b);
                return q;
            },
            Int2ObjectMaps::unmodifiable
        );
    }
}

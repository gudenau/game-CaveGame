package net.gudenau.cavegame.codec.impl;

import net.gudenau.cavegame.codec.Codec;
import net.gudenau.cavegame.codec.CodecResult;
import net.gudenau.cavegame.codec.ops.Operations;
import net.gudenau.cavegame.util.collection.SharedSoftMap;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class EnumCodec<T extends Enum<T>> implements Codec<T> {
    private static final Map<Class<? extends Enum<?>>, EnumCodec<?>> CACHE = new SharedSoftMap<>();

    private final Class<T> type;
    private final String name;
    private final Map<T, String> names;
    private final Map<String, T> values;

    private EnumCodec(Class<T> type) {
        this.type = type;
        name = type.getSimpleName();
        names = Stream.of(type.getEnumConstants())
            .collect(Collectors.toUnmodifiableMap(
                Function.identity(),
                (value) -> value.name().toLowerCase(Locale.ROOT)
            ));
        values = names.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(
                Map.Entry::getValue,
                Map.Entry::getKey
            ));
    }

    public static <T extends Enum<T>> Codec<T> of(Class<T> type) {
        return CodecCache.get(type).orElseGet(() -> CodecCache.put(new EnumCodec<>(type)));
    }

    @Override
    public <R> CodecResult<T> decode(Operations<R> operations, R input) {
        return operations.toString(input).flatMap((name) -> {
            var value = values.get(name);
            return value == null ? CodecResult.error(() -> name + " is not a " + name) : CodecResult.success(value);
        });
    }

    @Override
    public <R> CodecResult<R> encode(Operations<R> operations, T input, R prefix) {
        if(!Objects.equals(prefix, operations.blank())) {
            return CodecResult.error(() -> name + " can not have a prefix");
        }

        if(input == null) {
            return CodecResult.error(() -> name + " had a null value");
        }

        var name = names.get(input);
        if(name == null) {
            throw new AssertionError(this.name + " had an unknown enum value");
        }
        return CodecResult.success(operations.fromString(name));
    }

    @Override
    @NotNull
    public Class<T> type() {
        return type;
    }
}

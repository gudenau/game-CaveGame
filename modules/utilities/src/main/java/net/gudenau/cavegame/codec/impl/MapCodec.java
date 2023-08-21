package net.gudenau.cavegame.codec.impl;

import net.gudenau.cavegame.codec.Codec;
import net.gudenau.cavegame.codec.CodecResult;
import net.gudenau.cavegame.codec.ops.Operations;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MapCodec<K, V> implements Codec<Map<K, V>> {
    private final Codec<K> key;
    private final Codec<V> value;

    private MapCodec(Codec<K> key, Codec<V> value) {
        this.key = key;
        this.value = value;
    }

    //TODO Is there a better way
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    @NotNull
    public Class<Map<K, V>> type() {
        return (Class) Map.class;
    }

    public static <K, V> MapCodec<K, V> of(Codec<K> key, Codec<V> value) {
        return new MapCodec<>(key, value);
    }

    @Override
    public <R> CodecResult<Map<K, V>> decode(Operations<R> operations, R input) {
        return operations.toMap(input).flatMap((map) -> {
            List<CodecResult<Map.Entry<K, V>>> mapped = map.entrySet().stream()
                .map((entry) -> {
                    var k = key.decode(operations, entry.getKey());
                    var v = value.decode(operations, entry.getValue());

                    if(k.hasResult() && v.hasResult()) {
                        return CodecResult.success(Map.entry(
                            k.getResult(),
                            v.getResult()
                        ));
                    } else {
                        List<String> errors = new ArrayList<>(2);
                        boolean result = (k.hasResult() || k.getPartial().hasResult()) &&
                            (v.hasResult() || v.getPartial().hasResult());
                        if(k.hasPartial()) {
                            errors.add(k.getPartial().error());
                        }
                        if(v.hasPartial()) {
                            errors.add(v.getPartial().error());
                        }
                        Supplier<String> error = () -> String.join(", ", errors);
                        if(result) {
                            return CodecResult.error(Map.entry(
                                k.getResultOrPartial(),
                                v.getResultOrPartial()
                            ), error);
                        } else {
                            return CodecResult.<Map.Entry<K, V>>error(error);
                        }
                    }
                })
                .toList();
            List<String> errors = mapped.stream()
                .<String>mapMulti((value, consumer) -> {
                    if(value.hasPartial()) {
                        consumer.accept(value.getPartial().error());
                    }
                })
                .toList();
            var values = mapped.stream()
                .<Map.Entry<K, V>>mapMulti((result, consumer) -> {
                    (result.hasResult() ? result.result() : result.getPartial().result()).ifPresent(consumer);
                })
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
            if(errors.isEmpty()) {
                return CodecResult.success(values);
            } else {
                return CodecResult.error(values, () -> String.join(", ", errors));
            }
        });
    }

    @Override
    public <R> CodecResult<R> encode(Operations<R> operations, Map<K, V> input, R prefix) {
        List<CodecResult<Map.Entry<R, R>>> mapped = input.entrySet().stream()
            .map((entry) -> {
                var k = key.encode(operations, entry.getKey());
                var v = value.encode(operations, entry.getValue());

                if(k.hasResult() && v.hasResult()) {
                    return CodecResult.success(Map.entry(
                        k.getResult(),
                        v.getResult()
                    ));
                }

                var result = (k.hasResult() || k.getPartial().hasResult()) &&
                    (v.hasResult() || v.getPartial().hasResult());
                var errors = new ArrayList<String>(2);
                if(k.hasPartial()) {
                    errors.add(k.getPartial().error());
                }
                if(v.hasPartial()) {
                    errors.add(v.getPartial().error());
                }
                Supplier<String> error = () -> String.join(", ", errors);

                if(result) {
                    return CodecResult.error(Map.entry(k.getResultOrPartial(), v.getResultOrPartial()), error);
                } else {
                    return CodecResult.<Map.Entry<R, R>>error(error);
                }
            })
            .toList();
        var errors = mapped.stream()
            .<String>mapMulti((value, consumer) -> {
                if(value.hasPartial()) {
                    consumer.accept(value.getPartial().error());
                }
            })
            .toList();
        var values = mapped.stream()
            .<Map.Entry<R, R>>mapMulti((value, consumer) ->
                (value.hasResult() ? value.result() : value.getPartial().result()).ifPresent(consumer)
            )
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
        var map = operations.fromMap(values);
        if(errors.isEmpty()) {
            return CodecResult.success(map);
        } else {
            return CodecResult.error(map, () -> String.join(", ", errors));
        }
    }
}

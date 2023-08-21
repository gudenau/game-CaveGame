package net.gudenau.cavegame.codec.impl;

import net.gudenau.cavegame.codec.Codec;
import net.gudenau.cavegame.codec.CodecResult;
import net.gudenau.cavegame.codec.ops.Operations;
import net.gudenau.cavegame.util.collection.SharedSoftMap;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public final class ListCodec<T> implements Codec<List<T>> {
    private static final Map<Codec<?>, ListCodec<?>> CACHE = new SharedSoftMap<>();

    private final Codec<T> base;

    private ListCodec(Codec<T> base) {
        this.base = base;
    }

    //TODO Is there a better way
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    @NotNull
    public Class<List<T>> type() {
        return (Class) List.class;
    }

    @SuppressWarnings("unchecked")
    public static <T> Codec<List<T>> of(Codec<T> base) {
        return (ListCodec<T>) CACHE.computeIfAbsent(base, ListCodec::new);
    }

    @Override
    public <R> CodecResult<List<T>> decode(Operations<R> operations, R input) {
        return operations.toList(input).flatMap((list) -> {
            var mapped = list.stream()
                .map((element) -> base.decode(operations, element))
                .toList();
            List<String> errors = mapped.stream()
                .<String>mapMulti((value, consumer) -> {
                    if(value.hasPartial()) {
                        consumer.accept(value.getPartial().error());
                    }
                })
                .toList();
            var values = mapped.stream()
                .<T>mapMulti((result, consumer) -> {
                    (result.hasResult() ? result.result() : result.getPartial().result()).ifPresent(consumer);
                })
                .toList();
            if(errors.isEmpty()) {
                return CodecResult.success(values);
            } else {
                return CodecResult.error(values, () -> String.join(", ", errors));
            }
        });
    }

    @Override
    public <R> CodecResult<R> encode(Operations<R> operations, List<T> input, R prefix) {
        var mapped = input.stream()
            .map((value) -> base.encode(operations, value))
            .toList();
        var errors = mapped.stream()
            .<String>mapMulti((value, consumer) -> {
                if(value.hasPartial()) {
                    consumer.accept(value.getPartial().error());
                }
            })
            .toList();
        var values = mapped.stream()
            .<R>mapMulti((value, consumer) ->
                (value.hasResult() ? value.result() : value.getPartial().result()).ifPresent(consumer)
            )
            .toList();
        var list = operations.fromList(values);
        if(errors.isEmpty()) {
            return CodecResult.success(list);
        } else {
            return CodecResult.error(list, () -> String.join(", ", errors));
        }
    }
}

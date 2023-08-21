package net.gudenau.cavegame.codec.impl;

import net.gudenau.cavegame.codec.Codec;
import net.gudenau.cavegame.codec.CodecBuilder;
import net.gudenau.cavegame.codec.CodecResult;
import net.gudenau.cavegame.codec.ops.Operations;
import net.gudenau.cavegame.util.Treachery;
import net.jodah.typetools.TypeResolver;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class CodecBuilderImpl<T> implements CodecBuilder<T> {
    private record Argument<T>(
        @NotNull String name,
        @NotNull Codec<T> codec,
        @NotNull Class<T> type,
        boolean required
    ) {}

    private final List<Argument<?>> arguments = new ArrayList<>();

    private <A> void argument(String name, Codec<A> codec, Supplier<A> getter, boolean required) {
        arguments.add(new Argument<>(name, codec, codec.type(), required));
    }

    @Override
    @NotNull
    public <A> CodecBuilder<T> required(@NotNull String name, @NotNull Codec<A> codec, @NotNull Supplier<A> getter) {
        argument(name, codec, getter, true);
        return this;
    }

    @Override
    @NotNull
    public <A> CodecBuilder<T> optional(@NotNull String name, @NotNull Codec<A> codec, @NotNull Supplier<A> getter) {
        argument(name, codec, getter, false);
        return this;
    }

    @Override
    @NotNull
    public Codec<T> build(@NotNull MethodHandle factory) {
        //TODO Class gen
        return new Codec<>() {
            private final Class<T> type;
            {
                //noinspection unchecked
                type = (Class<T>) factory.type().returnType();
            }

            @Override
            @NotNull
            public Class<T> type() {
                return type;
            }

            @Override
            public <R> CodecResult<T> decode(Operations<R> operations, R input) {
                return operations.toMap(input).flatMap((inputMap) -> {
                    var values = new ArrayList<Object>(arguments.size());
                    var errors = new ArrayList<String>();
                    var state = new Object() {
                        boolean fatalError = false;
                    };
                    arguments.forEach((argument) -> {
                        var name = argument.name();

                        var inputValue = inputMap.get(operations.fromString(name));
                        if(inputValue == null) {
                            if (!argument.required()) {
                                values.add(null);
                                return;
                            }

                            errors.add("Required argument " + name + " is missing");
                            state.fatalError = true;
                        }

                        var convertedValue = argument.codec.decode(operations, inputValue);
                        if(convertedValue.hasResult()) {
                            values.add(convertedValue.getResult());
                            return;
                        }

                        var partial = convertedValue.getPartial();
                        if(partial.hasResult()) {
                            values.add(partial.getResult());
                        } else {
                            state.fatalError = true;
                        }
                        errors.add(partial.error());
                    });

                    if(state.fatalError) {
                        return CodecResult.error(() -> String.join(", ", errors));
                    }

                    T instance;
                    try {
                        //noinspection unchecked
                        instance = (T) factory.invokeWithArguments(values);
                    } catch (Throwable e) {
                        return CodecResult.error(() -> "Failed to construct " + Treachery.longClassName(factory.type().returnType()) + ": " + e.getMessage());
                    }

                    if(errors.isEmpty()) {
                        return CodecResult.success(instance);
                    } else {
                        return CodecResult.error(instance, () -> String.join(", ", errors));
                    }
                });
            }

            @Override
            public <R> CodecResult<R> encode(Operations<R> operations, T input, R prefix) {
                return null;
            }
        };
    }

    @Override
    @NotNull
    public Codec<T> build(Class<T> type) {
        try {
            var codec = build(Treachery.constructor(
                type,
                MethodType.methodType(
                    void.class,
                    arguments.stream()
                        .map(Argument::type)
                        .toArray(Class[]::new))
                )
            );
            return CodecCache.put(codec);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Failed to find constructor for " + Treachery.longClassName(type), e);
        }
    }
}

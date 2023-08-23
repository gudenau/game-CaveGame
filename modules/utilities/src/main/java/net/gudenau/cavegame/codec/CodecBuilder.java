package net.gudenau.cavegame.codec;

import net.gudenau.cavegame.annotations.Optional;
import net.gudenau.cavegame.annotations.Required;
import net.gudenau.cavegame.codec.impl.CodecBuilderImpl;
import net.gudenau.cavegame.codec.impl.CodecCache;
import net.gudenau.cavegame.codec.impl.EnumCodec;
import net.gudenau.cavegame.util.Treachery;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.util.function.Function;
import java.util.function.Supplier;

public sealed interface CodecBuilder<T> permits CodecBuilderImpl {
    @NotNull
    @Contract("-> new")
    static <T> CodecBuilder<T> builder() {
        return new CodecBuilderImpl<>();
    }

    @NotNull
    static <T extends Enum<T>> Codec<T> ofEnum(@NotNull Class<T> type) {
        return CodecCache.get(type).orElseGet(() -> EnumCodec.of(type));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @NotNull
    static <T extends Record> Codec<T> record(@NotNull Class<T> record) {
        var cached = CodecCache.get(record);
        if(cached.isPresent()) {
            return cached.get();
        }

        var builder = CodecBuilder.<T>builder();
        for (var component : record.getRecordComponents()) {
            boolean required = component.isAnnotationPresent(Required.class);
            boolean optional = component.isAnnotationPresent(Optional.class);
            if(required == optional) {
                if(required) {
                    throw new IllegalArgumentException("Component " + component.getName() + " can't be required and optional");
                } else {
                    throw new IllegalArgumentException("Component " + component.getName() + " must be required or optional");
                }
            }

            var codec = CodecCache.find(component.getType());

            var getterHandle = Treachery.unreflect(component.getAccessor());
            Function getter = (instance) -> {
                try {
                    return getterHandle.invoke(instance);
                } catch (Throwable e) {
                    throw new RuntimeException("Failed to invoke getter for " + component.getName());
                }
            };

            if(required) {
                builder.required(component.getName(), codec, getter);
            } else {
                builder.optional(component.getName(), codec, getter);
            }
        }
        return builder.build(record);
    }

    @NotNull
    @Contract("_, _, _ -> this")
    <A> CodecBuilder<T> required(@NotNull String name, @NotNull Codec<A> codec, @NotNull Function<T, A> getter);

    @NotNull
    @Contract("_, _, _ -> this")
    <A> CodecBuilder<T> optional(@NotNull String name, @NotNull Codec<A> codec, @NotNull Function<T, A> getter);

    @NotNull
    Codec<T> build(@NotNull MethodHandle factory);

    @NotNull
    Codec<T> build(Class<T> type);
}

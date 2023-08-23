package net.gudenau.cavegame.codec;

import net.gudenau.cavegame.codec.impl.ListCodec;
import net.gudenau.cavegame.codec.impl.MapCodec;
import net.gudenau.cavegame.codec.impl.PrimitiveCodec;
import net.gudenau.cavegame.codec.ops.Operations;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface Codec<T> extends Decoder<T>, Encoder<T> {
    Codec<Byte> BYTE = PrimitiveCodec.BYTE;
    Codec<Short> SHORT = PrimitiveCodec.SHORT;
    Codec<Integer> INT = PrimitiveCodec.INT;
    Codec<Long> LONG = PrimitiveCodec.LONG;
    Codec<Float> FLOAT = PrimitiveCodec.FLOAT;
    Codec<Double> DOUBLE = PrimitiveCodec.DOUBLE;
    Codec<String> STRING = PrimitiveCodec.STRING;

    @NotNull
    @Contract(pure = true)
    Class<T> type();

    @NotNull
    default Codec<List<T>> list() {
        return list(this);
    }

    static <T> Codec<List<T>> list(Codec<T> base) {
        return ListCodec.of(base);
    }

    static <K, V> Codec<Map<K, V>> map(Codec<K> key, Codec<V> value) {
        return MapCodec.of(key, value);
    }

    static <R> Codec<Map<R, R>> map(Codec<R> base) {
        return MapCodec.of(base, base);
    }

    interface Impartial<T> extends Codec<T> {
        @Override
        <R> CodecResult<R> encode(Operations<R> operations, T input);

        @Override
        default <R> CodecResult<R> encode(Operations<R> operations, T input, R prefix) {
            if(Objects.equals(prefix, operations.blank())) {
                return encode(operations, input);
            } else {
                return CodecResult.error(() -> "Unable to handle partial result: " + prefix);
            }
        }
    }
}

package net.gudenau.cavegame.codec;

import net.gudenau.cavegame.codec.impl.EnumCodec;
import net.gudenau.cavegame.codec.impl.ListCodec;
import net.gudenau.cavegame.codec.impl.MapCodec;
import net.gudenau.cavegame.codec.impl.PrimitiveCodec;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

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

    static <K, V> Codec<Map<K, V>> of(Codec<K> key, Codec<V> value) {
        return MapCodec.of(key, value);
    }

    static <R> Codec<Map<R, R>> of(Codec<R> base) {
        return MapCodec.of(base, base);
    }
}

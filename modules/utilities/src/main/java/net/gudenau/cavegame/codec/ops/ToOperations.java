package net.gudenau.cavegame.codec.ops;

import net.gudenau.cavegame.codec.CodecResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface ToOperations<T> {
    @NotNull
    CodecResult<Number> toNumber(@NotNull T value);

    @NotNull
    default CodecResult<Byte> toByte(T value) {
        return toNumber(value).map(Number::byteValue);
    }

    @NotNull
    default CodecResult<Short> toShort(T value) {
        return toNumber(value).map(Number::shortValue);
    }

    @NotNull
    default CodecResult<Integer> toInt(T value) {
        return toNumber(value).map(Number::intValue);
    }

    @NotNull
    default CodecResult<Long> toLong(T value) {
        return toNumber(value).map(Number::longValue);
    }

    @NotNull
    default CodecResult<Float> toFloat(T value) {
        return toNumber(value).map(Number::floatValue);
    }

    @NotNull
    default CodecResult<Double> toDouble(T value) {
        return toNumber(value).map(Number::doubleValue);
    }

    @NotNull
    default CodecResult<Boolean> toBoolean(T value) {
        return toNumber(value).map((number) -> number.byteValue() != 0);
    }

    @NotNull
    CodecResult<String> toString(T value);

    @NotNull
    CodecResult<List<T>> toList(T value);

    @NotNull
    CodecResult<Map<T, T>> toMap(T value);

    @NotNull
    CodecResult<Stream<T>> toStream(T value);

    @NotNull
    CodecResult<Stream<Map.Entry<T, T>>> toEntryStream(T value);
}

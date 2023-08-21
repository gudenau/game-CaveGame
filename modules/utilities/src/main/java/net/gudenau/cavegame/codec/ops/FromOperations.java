package net.gudenau.cavegame.codec.ops;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public interface FromOperations<T> {
    T fromNumber(@NotNull Number value);

    default T fromByte(byte value) {
        return fromNumber(value);
    }

    default T fromShort(short value) {
        return fromNumber(value);
    }

    default T fromInt(int value) {
        return fromNumber(value);
    }

    default T fromLong(long value) {
        return fromNumber(value);
    }

    default T fromFloat(float value) {
        return fromNumber(value);
    }

    default T fromDouble(double value) {
        return fromNumber(value);
    }

    default T fromBoolean(boolean value) {
        return fromNumber((byte) (value ? 1 : 0));
    }

    T fromString(@NotNull String value);

    default T fromList() {
        return fromList(List.of());
    }

    T fromList(@NotNull List<T> list);

    default T fromMap() {
        return fromMap(Map.of());
    }

    T fromMap(@NotNull Map<T, T> map);
}

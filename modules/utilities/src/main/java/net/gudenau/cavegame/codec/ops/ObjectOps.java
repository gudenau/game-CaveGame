package net.gudenau.cavegame.codec.ops;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.gudenau.cavegame.codec.CodecResult;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ObjectOps implements Operations<Object> {
    public static final ObjectOps INSTANCE = new ObjectOps();

    private ObjectOps() {
        if(INSTANCE != null) {
            throw new AssertionError();
        }
    }

    @Override
    public Object fromNumber(@NotNull Number value) {
        return value;
    }

    @Override
    public Object fromString(@NotNull String value) {
        return value;
    }

    @Override
    public Object fromList(@NotNull List<Object> list) {
        return list;
    }

    @Override
    public Object fromMap(@NotNull Map<Object, Object> map) {
        return map;
    }

    @Override
    public Object blank() {
        return null;
    }

    @Override
    public <R> R convert(Operations<R> other, Object input) {
        return switch(input) {
            case Map<?, ?> map -> convertMap(other, map);
            case Collection<?> array -> convertList(other, array);
            case String string -> other.fromString(string);
            case Byte value -> other.fromByte(value);
            case Short value -> other.fromShort(value);
            case Integer value -> other.fromInt(value);
            case Long value -> other.fromLong(value);
            case Float value -> other.fromFloat(value);
            case Double value -> other.fromDouble(value);
            case Boolean value -> other.fromBoolean(value);
            case null -> other.blank();
            default -> throw new AssertionError();
        };
    }

    @Override
    public Object createList(Stream<Object> stream) {
        return stream.toList();
    }

    @Override
    public Object createMap(Stream<Map.Entry<Object, Object>> stream) {
        return stream.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private <R> CodecResult<R> to(Object value, Class<R> type) {
        if(type.isInstance(value)) {
            return CodecResult.success(type.cast(value));
        } else {
            return CodecResult.error(() -> "Expected " + type.getSimpleName() + " but got " + value.getClass().getSimpleName());
        }
    }

    @Override
    public @NotNull CodecResult<Number> toNumber(@NotNull Object value) {
        return to(value, Number.class);
    }

    @Override
    public @NotNull CodecResult<String> toString(Object value) {
        return to(value, String.class);
    }

    @Override
    public @NotNull CodecResult<List<Object>> toList(Object value) {
        return (CodecResult<List<Object>>)(Object) to(value, Map.class);
    }

    @Override
    public @NotNull CodecResult<Map<Object, Object>> toMap(Object value) {
        return (CodecResult<Map<Object, Object>>)(Object) to(value, Map.class);
    }

    @Override
    public @NotNull CodecResult<Stream<Object>> toStream(Object value) {
        return to(value, List.class).map(List::stream);
    }

    @Override
    public @NotNull CodecResult<Stream<Map.Entry<Object, Object>>> toEntryStream(Object value) {
        return to(value, Map.class).map((v) -> v.entrySet().stream());
    }
}

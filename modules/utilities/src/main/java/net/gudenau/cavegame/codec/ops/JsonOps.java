package net.gudenau.cavegame.codec.ops;

import com.google.gson.*;
import net.gudenau.cavegame.codec.CodecResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class JsonOps implements Operations<JsonElement> {
    public static final JsonOps INSTANCE = new JsonOps();

    private JsonOps() {
        if(INSTANCE != null) {
            throw new AssertionError();
        }
    }

    @Override
    public JsonElement fromNumber(@NotNull Number value) {
        return new JsonPrimitive(value);
    }

    @Override
    public JsonElement fromBoolean(boolean value) {
        return new JsonPrimitive(value);
    }

    @Override
    public JsonElement fromString(@NotNull String value) {
        return new JsonPrimitive(value);
    }

    @Override
    public JsonElement fromList(@NotNull List<JsonElement> list) {
        var result = new JsonArray(list.size());
        list.forEach(result::add);
        return result;
    }

    @Override
    public JsonElement fromMap(@NotNull Map<JsonElement, JsonElement> map) {
        var result = new JsonObject();
        map.forEach((key, value) -> result.add(key.getAsString(), value));
        return result;
    }

    @Override
    public JsonElement blank() {
        return JsonNull.INSTANCE;
    }

    @Override
    public <U> U convert(Operations<U> other, JsonElement input) {
        return switch(input) {
            case JsonObject object -> convertMap(other, object);
            case JsonArray array -> convertList(other, array);
            case JsonPrimitive primitive -> {
                if(primitive.isString()) {
                    yield other.fromString(primitive.getAsString());
                } else if(primitive.isNumber()) {
                    yield switch (primitive.getAsNumber()) {
                        case Byte value -> other.fromByte(value);
                        case Short value -> other.fromShort(value);
                        case Integer value -> other.fromInt(value);
                        case Long value -> other.fromLong(value);
                        case Float value -> other.fromFloat(value);
                        case Double value -> other.fromDouble(value);
                        default -> throw new AssertionError();
                    };
                } else {
                    yield other.fromBoolean(primitive.getAsBoolean());
                }
            }
            case JsonNull unused -> other.blank();
            default -> throw new AssertionError();
        };
    }

    @Override
    public JsonElement createList(Stream<JsonElement> stream) {
        var array = new JsonArray();
        stream.forEach(array::add);
        return array;
    }

    @Override
    public JsonElement createMap(Stream<Map.Entry<JsonElement, JsonElement>> stream) {
        var object = new JsonObject();
        stream.forEach((entry) -> object.add(entry.getKey().getAsString(), entry.getValue()));
        return object;
    }

    @Override
    @NotNull
    public CodecResult<Number> toNumber(@NotNull JsonElement value) {
        if(value instanceof JsonPrimitive primitive && primitive.isNumber()) {
            return CodecResult.success(primitive.getAsNumber());
        } else {
            return CodecResult.error(() -> "value was not a number, got " + value.getClass().getSimpleName() + " instead");
        }
    }

    @Override
    @NotNull
    public CodecResult<String> toString(JsonElement value) {
        if(value instanceof JsonPrimitive primitive && primitive.isString()) {
            return CodecResult.success(primitive.getAsString());
        } else {
            return CodecResult.error(() -> "value was not a string, got " + value.getClass().getSimpleName() + " instead");
        }
    }

    @Override
    @NotNull
    public CodecResult<List<JsonElement>> toList(JsonElement value) {
        if(!(value instanceof JsonArray array)) {
            return CodecResult.error(() -> "value was not an array, got " + value.getClass().getSimpleName() + " instead");
        }

        return CodecResult.success(array.asList());
    }

    @Override
    @NotNull
    public CodecResult<Map<JsonElement, JsonElement>> toMap(JsonElement value) {
        if(!(value instanceof JsonObject object)) {
            return CodecResult.error(() -> "value was not an object, got " + value.getClass().getSimpleName() + " instead");
        }

        return CodecResult.success(object.asMap().entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(
                (entry) -> fromString(entry.getKey()),
                Map.Entry::getValue
            ))
        );
    }

    @Override
    @NotNull
    public CodecResult<Stream<JsonElement>> toStream(JsonElement value) {
        if(!(value instanceof JsonArray array)) {
            return CodecResult.error(() -> "value was not an array, got " + value.getClass().getSimpleName() + " instead");
        }

        return CodecResult.success(array.asList().stream());
    }

    @Override
    @NotNull
    public CodecResult<Stream<Map.Entry<JsonElement, JsonElement>>> toEntryStream(JsonElement value) {
        if(!(value instanceof JsonObject object)) {
            return CodecResult.error(() -> "value was not an object, got " + value.getClass().getSimpleName() + " instead");
        }

        return CodecResult.success(object.asMap().entrySet().stream()
            .map((entry) -> Map.entry(fromString(entry.getKey()), entry.getValue()))
        );
    }
}

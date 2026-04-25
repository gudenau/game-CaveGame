package net.gudenau.cavegame.renderer.texture;

import net.gudenau.cavegame.resource.Identifier;
import net.gudenau.codec.Codec;
import net.gudenau.codec.Decoder;
import net.gudenau.codec.Encoder;
import net.gudenau.codec.Operations;
import net.gudenau.codec.util.Result;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
[
  {
    "id": "button",
    "texture": "button",
    "data": {
      "type": "nine_square",
      "size": 9,
      "border": 4
    }
  }
]
 */
public record AtlasMetadata(
    @NotNull Identifier id,
    @NotNull Identifier texture,
    @NotNull Data data
) {
    public static final Codec<AtlasMetadata> CODEC = Codec.record((instance) -> instance.of(
        AtlasMetadata::new,
        Identifier.CODEC.of("id", AtlasMetadata::id),
        Identifier.CODEC.of("texture", AtlasMetadata::texture),
        Data.CODEC.of("data", AtlasMetadata::data)
    ));
    public static final Codec<List<AtlasMetadata>> CODEC_LIST = CODEC.list();

    public sealed interface Data {
        Codec<Data> CODEC = Codec.dispatch(Codec.STRING.of("type", (data) -> switch(data) {
            case NineSquare _ -> "nine_square";
        }), (type) -> switch(type) {
            case "nine_square" -> Result.success(NineSquare.CODEC);
            default -> Result.error(() -> "Unknown type " + type);
        });
    }

    public record NineSquare(
        int width,
        int height,
        int borderWidth,
        int borderHeight
    ) implements Data {
        public static final Codec<NineSquare> CODEC = Codec.of(new Encoder<>() {
            @Override
            @NotNull
            public <T> Result<T> encode(@NotNull Operations<T> ops, @NotNull NineSquare input) {
                var elements = new HashMap<T, T>();

                if(input.width() == input.height()) {
                    elements.put(ops.createStringUnchecked("size"), ops.createIntUnchecked(input.width()));
                } else {
                    elements.put(ops.createStringUnchecked("width"), ops.createIntUnchecked(input.width()));
                    elements.put(ops.createStringUnchecked("height"), ops.createIntUnchecked(input.height()));
                }

                if(input.borderWidth() == input.borderHeight()) {
                    elements.put(ops.createStringUnchecked("border"), ops.createIntUnchecked(input.borderWidth()));
                } else {
                    elements.put(ops.createStringUnchecked("borderWidth"), ops.createIntUnchecked(input.borderWidth()));
                    elements.put(ops.createStringUnchecked("borderHeight"), ops.createIntUnchecked(input.borderHeight()));
                }

                return ops.createMap(elements);
            }
        }, new Decoder<>() {
            @Override
            @NotNull
            public <T> Result<NineSquare> decode(@NotNull Operations<T> ops, @NotNull T input) {
                Map<T, T> map;
                switch (ops.getMap(input)) {
                    case Result.Value(var value) -> map = value;
                    case Result.Error(var error, var _) -> {
                        return Result.error(() -> "Failed to decode AtlasMetadata.Data: " + error.get());
                    }
                }

                int width;
                int height;
                if(map.get(ops.createStringUnchecked("size")) != null) {
                    switch (ops.getInt(map.get(ops.createStringUnchecked("size")))) {
                        case Result.Value(var value) -> width = height = value;
                        case Result.Error(var error, var _) -> {
                            return Result.error(() -> "Failed to decode AtlasMetadata.Data: " + error.get());
                        }
                    }
                } else if (
                    map.get(ops.createStringUnchecked("width")) != null &&
                    map.get(ops.createStringUnchecked("height")) != null
                ) {
                    switch (ops.getInt(map.get(ops.createStringUnchecked("width")))) {
                        case Result.Value(var value) -> width = value;
                        case Result.Error(var error, var _) -> {
                            return Result.error(() -> "Failed to decode AtlasMetadata.Data: " + error.get());
                        }
                    }

                    switch (ops.getInt(map.get(ops.createStringUnchecked("height")))) {
                        case Result.Value(var value) -> height = value;
                        case Result.Error(var error, var _) -> {
                            return Result.error(() -> "Failed to decode AtlasMetadata.Data: " + error.get());
                        }
                    }
                } else {
                    return Result.error(() -> "Failed to decode AtlasMetadata.Data: size or width/height not specified");
                }

                int borderWidth;
                int borderHeight;
                if(map.get(ops.createStringUnchecked("border")) != null) {
                    switch (ops.getInt(map.get(ops.createStringUnchecked("border")))) {
                        case Result.Value(var value) -> borderWidth = borderHeight = value;
                        case Result.Error(var error, var _) -> {
                            return Result.error(() -> "Failed to decode AtlasMetadata.Data: " + error.get());
                        }
                    }
                } else if (
                    map.get(ops.createStringUnchecked("borderWidth")) != null &&
                    map.get(ops.createStringUnchecked("borderHeight")) != null
                ) {
                    switch (ops.getInt(map.get(ops.createStringUnchecked("borderWidth")))) {
                        case Result.Value(var value) -> borderWidth = value;
                        case Result.Error(var error, var _) -> {
                            return Result.error(() -> "Failed to decode AtlasMetadata.Data: " + error.get());
                        }
                    }

                    switch (ops.getInt(map.get(ops.createStringUnchecked("borderHeight")))) {
                        case Result.Value(var value) -> borderHeight = value;
                        case Result.Error(var error, var _) -> {
                            return Result.error(() -> "Failed to decode AtlasMetadata.Data: " + error.get());
                        }
                    }
                } else {
                    return Result.error(() ->
                        "Failed to decode AtlasMetadata.Data: border or borderWidth/borderHeight not specified"
                    );
                }

                return Result.success(new NineSquare(width, height, borderWidth, borderHeight));
            }
        });

        public NineSquare(int size, int border) {
            this(size, size, border, border);
        }
    }
}

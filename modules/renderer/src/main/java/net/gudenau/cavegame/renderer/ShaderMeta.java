package net.gudenau.cavegame.renderer;

import net.gudenau.cavegame.annotations.Required;
import net.gudenau.cavegame.codec.Codec;
import net.gudenau.cavegame.codec.CodecBuilder;
import net.gudenau.cavegame.codec.CodecResult;
import net.gudenau.cavegame.codec.ops.JsonOps;
import net.gudenau.cavegame.resource.Identifier;
import net.gudenau.cavegame.resource.ResourceLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;

public record ShaderMeta(
    @NotNull Map<String, Attribute> attributes,
    @NotNull Map<String, Identifier> files
) {
    public static final Codec<ShaderMeta> CODEC = CodecBuilder.<ShaderMeta>builder()
        .optional("attributes", Codec.map(Codec.STRING, Attribute.CODEC), ShaderMeta::attributes)
        .required("files", Codec.map(Codec.STRING, Identifier.CODEC), ShaderMeta::files)
        .build(ShaderMeta.class);
    public static final Codec<Map<ShaderType, ShaderMeta>> MAP_CODEC = Codec.map(ShaderType.CODEC, CODEC);

    public ShaderMeta(@Nullable Map<String, Attribute> attributes, @NotNull Map<String, Identifier> files) {
        this.attributes = attributes == null ? Map.of() : attributes;
        this.files = files;
    }

    public static CodecResult<Map<ShaderType, ShaderMeta>> load(@NotNull Identifier metadata) {
        var normalized = metadata.normalize("shader", ".json");
        try(var reader = ResourceLoader.reader(normalized)) {
            return JsonOps.decode(reader, MAP_CODEC);
        } catch (IOException e) {
            return CodecResult.error("Failed to read metadata for " + metadata, e);
        }
    }

    public record Attribute(
        @Required @NotNull Type type
    ) {
        public enum Type {
            POSITION,
            COLOR,
            ;

            public static final Codec<Type> CODEC = CodecBuilder.ofEnum(Type.class);
        }

        public static final Codec<Attribute> CODEC = CodecBuilder.record(Attribute.class);
    }
}

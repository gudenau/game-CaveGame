package net.gudenau.cavegame.renderer.shader;

import net.gudenau.cavegame.annotations.Optional;
import net.gudenau.cavegame.annotations.Required;
import net.gudenau.cavegame.codec.Codec;
import net.gudenau.cavegame.codec.CodecBuilder;
import net.gudenau.cavegame.codec.CodecResult;
import net.gudenau.cavegame.codec.ops.JsonOps;
import net.gudenau.cavegame.renderer.texture.TextureFormat;
import net.gudenau.cavegame.resource.Identifier;
import net.gudenau.cavegame.resource.ResourceLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JFormattedTextField;
import java.io.IOException;
import java.util.Map;

public record ShaderMeta(
    @NotNull Map<String, Attribute> attributes,
    @NotNull Map<String, Uniform> uniforms,
    @NotNull Map<ShaderType, Shader> shaders,
    @NotNull Map<String, Texture> textures
) {
    public static final Codec<ShaderMeta> CODEC = CodecBuilder.<ShaderMeta>builder()
        .optional("attributes", Codec.map(Codec.STRING, Attribute.CODEC), ShaderMeta::attributes)
        .optional("uniforms", Codec.map(Codec.STRING, Uniform.CODEC), ShaderMeta::uniforms)
        .required("shaders", Codec.map(ShaderType.CODEC, Shader.CODEC), ShaderMeta::shaders)
        .optional("textures", Codec.map(Codec.STRING, Texture.CODEC), ShaderMeta::textures)
        .build(ShaderMeta.class);

    public ShaderMeta(
        @Nullable Map<String, Attribute> attributes,
        @Nullable Map<String, Uniform> uniforms,
        @NotNull Map<ShaderType, Shader> shaders,
        @Nullable Map<String, Texture> textures
    ) {
        this.attributes = attributes == null ? Map.of() : attributes;
        this.uniforms = uniforms == null ? Map.of() : uniforms;
        this.shaders = shaders;
        this.textures = textures == null ? Map.of() : textures;
    }

    public static CodecResult<ShaderMeta> load(@NotNull Identifier metadata) {
        var normalized = metadata.normalize("shader", ".json");
        try(var reader = ResourceLoader.reader(normalized)) {
            return JsonOps.decode(reader, CODEC);
        } catch (IOException e) {
            return CodecResult.error("Failed to read metadata for " + metadata, e);
        }
    }

    public record Attribute(
        @Required @NotNull AttributeUsage usage
    ) {
        public static final Codec<Attribute> CODEC = CodecBuilder.record(Attribute.class);
    }

    public record Uniform(
        @Optional @Nullable UniformUsage usage,
        @Required @NotNull ShaderType shader
    ) {
        public static final Codec<Uniform> CODEC = CodecBuilder.record(Uniform.class);
    }

    public record Shader(
        @NotNull Map<String, Identifier> files
    ) {
        public static final Codec<Shader> CODEC = CodecBuilder.<Shader>builder()
            .required("files", Codec.map(Codec.STRING, Identifier.CODEC), Shader::files)
            .build(Shader.class);
    }

    public record Texture(
    ) {
        public static final Codec<Texture> CODEC = CodecBuilder.<Texture>builder()
            .build(Texture.class);
    }
}

package net.gudenau.cavegame.renderer.shader;

import net.gudenau.cavegame.annotations.Optional;
import net.gudenau.cavegame.annotations.Required;
import net.gudenau.cavegame.resource.Identifier;
import net.gudenau.cavegame.resource.ResourceLoader;
import net.gudenau.cavegame.util.CodecUtils;
import net.gudenau.codec.Codec;
import net.gudenau.codec.util.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;

public record ShaderMeta(
    @NotNull Map<String, Attribute> attributes,
    @NotNull Map<String, Uniform> uniforms,
    @NotNull Map<ShaderType, Shader> shaders,
    @NotNull Map<String, Texture> textures
) {
    public static final Codec<ShaderMeta> CODEC = Codec.record((instance) -> instance.of(
        ShaderMeta::new,
        Codec.map(Codec.STRING, Attribute.CODEC).of("attributes", ShaderMeta::attributes),
        Codec.map(Codec.STRING, Uniform.CODEC).of("uniforms", ShaderMeta::uniforms),
        Codec.map(ShaderType.CODEC, Shader.CODEC).of("shaders", ShaderMeta::shaders),
        Codec.map(Codec.STRING, Texture.CODEC).of("textures", ShaderMeta::textures)
    ));

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

    public static Result<ShaderMeta> load(@NotNull Identifier metadata) {
        var normalized = metadata.normalize("shader", ".json");
        try {
            var json = ResourceLoader.json(normalized);
            return CodecUtils.decode(CODEC, json);
        } catch (IOException e) {
            return CodecUtils.error("Failed to read metadata for " + metadata, e);
        }
    }

    public record Attribute(
        @Required @NotNull AttributeUsage usage
    ) {
        public static final Codec<Attribute> CODEC = Codec.record((instance) ->
            instance.of(
                Attribute::new,
                AttributeUsage.CODEC.of("usage", Attribute::usage)
            )
        );
    }

    public record Uniform(
        @Optional @Nullable UniformUsage usage,
        @Required @NotNull ShaderType shader
    ) {
        public static final Codec<Uniform> CODEC = Codec.record((instance) -> instance.of(
            Uniform::new,
            UniformUsage.CODEC.of("usage", Uniform::usage),
            ShaderType.CODEC.of("shader", Uniform::shader)
        ));
    }

    public record Shader(
        @NotNull Map<String, Identifier> files
    ) {
        public static final Codec<Shader> CODEC = Codec.record((instance) -> instance.of(
            Shader::new,
            Codec.map(Codec.STRING, Identifier.CODEC).of("files", Shader::files)
        ));
    }

    public record Texture() {
        public static final Codec<Texture> CODEC = Codec.empty(Texture::new);
    }
}

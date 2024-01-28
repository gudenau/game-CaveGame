package net.gudenau.cavegame.renderer.gl.shader;

import net.gudenau.cavegame.renderer.gl.GlState;
import net.gudenau.cavegame.renderer.shader.ShaderMeta;
import net.gudenau.cavegame.renderer.shader.VertexAttribute;
import net.gudenau.cavegame.renderer.shader.VertexFormat;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.system.MemoryUtil.NULL;

//FIXME Fix this junk
public final class GlVertexFormat implements VertexFormat {
    private final List<VertexAttribute> attributes;
    private final int stride;
    private final VertexAttribute color;
    private final VertexAttribute position;
    private final VertexAttribute textureCoord;
    private final int vao;

    public GlVertexFormat(GlState state, Map<String, GlProgram.Attribute> inputs, Map<String, ShaderMeta.Attribute> metadata) {
        Set<String> missing = new HashSet<>();

        List<VertexAttribute> attributes = new ArrayList<>();
        int offset = 0;

        VertexAttribute color = null;
        VertexAttribute position = null;
        VertexAttribute textureCoord = null;

        var inputAttributes = inputs.values().stream()
            .sorted(Comparator.comparingInt(GlProgram.Attribute::location))
            .toList();
        for(var input : inputAttributes) {
            var name = input.name();
            var meta = metadata.get(name);
            if(meta == null) {
                missing.add(name);
                continue;
            }

            var attribute = new GlVertexAttribute(input, meta, offset);
            offset += attribute.stride();
            attributes.add(attribute);
            var usage = attribute.usage();
            if(usage != null) {
                switch(usage) {
                    case COLOR -> color = attribute;
                    case POSITION -> position = attribute;
                    case TEXTURE_COORD -> textureCoord = attribute;
                }
            }
        }

        this.color = color;
        this.position = position;
        this.textureCoord = textureCoord;

        if(!missing.isEmpty()) {
            throw new RuntimeException(
                "Bad shader metadata, missing attributes: " +
                    String.join(", ", missing)
            );
        }

        this.attributes = Collections.unmodifiableList(attributes);
        this.stride = attributes.stream()
            .mapToInt(VertexAttribute::stride)
            .sum();

        vao = glGenVertexArrays();
    }

    @Override
    public @NotNull List<@NotNull VertexAttribute> attributes() {
        return attributes;
    }

    @Override
    public @NotNull Optional<VertexAttribute> color() {
        return Optional.ofNullable(color);
    }

    @Override
    public @NotNull Optional<VertexAttribute> position() {
        return Optional.ofNullable(position);
    }

    @Override
    public @NotNull Optional<VertexAttribute> textureCoord() {
        return Optional.ofNullable(textureCoord);
    }

    @Override
    public int stride() {
        return stride;
    }

    public void bind(@NotNull GlState state) {
        state.bindVao(vao);

        this.attributes.forEach((attribute) -> {
            glEnableVertexAttribArray(attribute.location());
            glVertexAttribPointer(attribute.location(), attribute.count(), switch(attribute.type()) {
                case STRUCT -> throw new IllegalArgumentException("Struct isn't supported as an attribute");
                case FLOAT -> GL_FLOAT;
                case SAMPLER -> throw new IllegalArgumentException("Sampler isn't supported as an attribute");
            }, false, attribute.stride(), attribute.offset());
        });
    }

    public void release(@NotNull GlState state) {
        if(state.boundVao() == vao) {
            state.bindVao(0);
        }
    }
}

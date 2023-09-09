package net.gudenau.cavegame.renderer.vk;

import net.gudenau.cavegame.renderer.shader.AttributeUsage;
import net.gudenau.cavegame.renderer.shader.ShaderMeta;
import net.gudenau.cavegame.renderer.shader.VertexAttribute;
import net.gudenau.cavegame.renderer.shader.VertexFormat;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class VkVertexFormat implements VertexFormat {
    private final List<VertexAttribute> attributes;
    private final int stride;
    private final VertexAttribute color;
    private final VertexAttribute position;

    public VkVertexFormat(@NotNull VulkanShaderModule vertex, @NotNull Map<String, ShaderMeta.Attribute> metadata) {
        Set<String> missing = new HashSet<>();

        List<VertexAttribute> attributes = new ArrayList<>();
        int offset = 0;

        VertexAttribute color = null;
        VertexAttribute position = null;

        var inputs = new ArrayList<>(vertex.inputs());
        inputs.sort(Comparator.comparingInt(VulkanShaderModule.Attribute::location));
        for(var input : vertex.inputs()) {
            var name = input.name();
            var meta = metadata.get(name);
            if(meta == null) {
                missing.add(name);
                continue;
            }

            var attribute = new VkVertexAttribute(input, meta, offset);
            offset += attribute.stride();
            attributes.add(attribute);
            switch(attribute.usage()) {
                case COLOR -> color = attribute;
                case POSITION -> position = attribute;
            }
        }

        this.color = color;
        this.position = position;

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
    }

    @Override
    @NotNull
    public List<VertexAttribute> attributes() {
        return attributes;
    }

    @Override
    @NotNull
    public Optional<VertexAttribute> color() {
        return Optional.of(color);
    }

    @Override
    @NotNull
    public Optional<VertexAttribute> position() {
        return Optional.of(position);
    }

    @Override
    public int stride() {
        return stride;
    }
}

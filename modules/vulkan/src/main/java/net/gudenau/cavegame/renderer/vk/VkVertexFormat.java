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
    private final Map<AttributeUsage, VertexAttribute> attributes;
    private final int stride;

    public VkVertexFormat(@NotNull VulkanShaderModule vertex, @NotNull Map<String, ShaderMeta.Attribute> metadata) {
        var inputs = vertex.inputs().stream().collect(Collectors.toUnmodifiableMap(
            VulkanShaderModule.Attribute::name,
            Function.identity()
        ));
        Set<String> missing = new HashSet<>(inputs.keySet());
        Set<String> extra = new HashSet<>();

        Map<AttributeUsage, VertexAttribute> attributes = new HashMap<>();
        int offset = 0;
        for(var entry : metadata.entrySet()) {
            var name = entry.getKey();
            var meta = entry.getValue();
            if(!missing.remove(name)) {
                extra.add(name);
                continue;
            }
            var input = inputs.get(name);

            var attribute = new VkVertexAttribute(input, meta, offset);
            offset += attribute.stride();
            attributes.put(meta.usage(), attribute);
        }

        if(!missing.isEmpty() || !extra.isEmpty()) {
            throw new RuntimeException(
                "Bad shader metadata, missing attributes: " +
                    String.join(", ", missing) +
                    "; extra attributes: " +
                    String.join(", ", extra)
            );
        }

        this.attributes = Collections.unmodifiableMap(attributes);
        this.stride = attributes.values().stream()
            .mapToInt(VertexAttribute::stride)
            .sum();
    }

    @Override
    @NotNull
    public Map<AttributeUsage, VertexAttribute> attributes() {
        return attributes;
    }

    @Override
    public int stride() {
        return stride;
    }
}

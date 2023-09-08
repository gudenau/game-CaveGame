package net.gudenau.cavegame.renderer.vk;

import net.gudenau.cavegame.renderer.shader.AttributeType;
import net.gudenau.cavegame.renderer.shader.ShaderMeta;
import net.gudenau.cavegame.renderer.shader.VertexAttribute;
import org.jetbrains.annotations.NotNull;

public record VkVertexAttribute(
    @NotNull String name,
    @NotNull AttributeType type,
    int count,
    int stride,
    int location,
    int offset
) implements VertexAttribute {
    public VkVertexAttribute(VulkanShaderModule.Attribute input, ShaderMeta.Attribute meta, int offset) {
        this(
            input.name(),
            input.type(),
            input.size(),
            input.stride(),
            input.location(),
            offset
        );
    }
}

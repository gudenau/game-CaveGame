package net.gudenau.cavegame.renderer.vk;

import net.gudenau.cavegame.renderer.shader.AttributeType;
import net.gudenau.cavegame.renderer.shader.AttributeUsage;
import net.gudenau.cavegame.renderer.shader.ShaderMeta;
import net.gudenau.cavegame.renderer.shader.VertexAttribute;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record VkVertexAttribute(
    @NotNull String name,
    @NotNull AttributeType type,
    int count,
    int stride,
    int location,
    @Nullable AttributeUsage usage,
    int offset
) implements VertexAttribute {
    public VkVertexAttribute(VulkanShaderModule.Resource input, ShaderMeta.Attribute meta, int offset) {
        this(
            input.name(),
            input.type(),
            input.size(),
            input.stride(),
            input.location(),
            meta.usage(),
            offset
        );
    }
}

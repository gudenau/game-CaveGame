package net.gudenau.cavegame.renderer.vk;

import net.gudenau.cavegame.renderer.shader.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record VkUniform(
    @NotNull String name,
    @NotNull AttributeType type,
    int count,
    int stride,
    int location,
    @Nullable UniformUsage usage,
    @NotNull ShaderType shader
) implements Uniform {
    public VkUniform(VulkanShaderModule.Resource input, ShaderMeta.Uniform meta) {
        this(
            input.name(),
            input.type(),
            input.size(),
            input.stride(),
            input.location(),
            meta.usage(),
            meta.shader()
        );
    }
}

package net.gudenau.cavegame.renderer.vk;

import net.gudenau.cavegame.renderer.shader.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public record VkUniform(
    @NotNull String name,
    @NotNull AttributeType type,
    int count,
    int stride,
    int location,
    @Nullable UniformUsage usage,
    @NotNull ShaderType shaderType
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

    @Override
    public void upload(@NotNull Consumer<ByteBuffer> consumer) {
        throw new UnsupportedOperationException();
    }
}

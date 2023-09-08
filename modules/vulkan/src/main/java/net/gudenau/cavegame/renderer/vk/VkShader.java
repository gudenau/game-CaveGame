package net.gudenau.cavegame.renderer.vk;

import net.gudenau.cavegame.renderer.BufferBuilder;
import net.gudenau.cavegame.renderer.BufferType;
import net.gudenau.cavegame.renderer.shader.Shader;
import net.gudenau.cavegame.renderer.shader.VertexFormat;
import org.jetbrains.annotations.NotNull;

public class VkShader implements Shader {
    private final VkRenderer renderer;
    private final VulkanGraphicsPipeline pipeline;
    private final VkVertexFormat vertexFormat;

    public VkShader(@NotNull VkRenderer renderer, @NotNull VulkanGraphicsPipeline pipeline, @NotNull VkVertexFormat vertexFormat) {
        this.renderer = renderer;
        this.pipeline = pipeline;
        this.vertexFormat = vertexFormat;
    }

    @Override
    public void close() {
        pipeline.close();
    }

    @NotNull
    @Override
    public BufferBuilder builder() {
        return BufferBuilder.create(this, (data) -> {
            var buffer = renderer.createBuffer(BufferType.VERTEX, data.remaining());
            buffer.shader(this);
            buffer.upload(data);
            return buffer;
        });
    }

    @Override
    public VertexFormat format() {
        return vertexFormat;
    }

    public VulkanGraphicsPipeline pipeline() {
        return pipeline;
    }
}

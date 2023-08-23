package net.gudenau.cavegame.renderer.vk;

import net.gudenau.cavegame.renderer.Shader;
import org.jetbrains.annotations.NotNull;

public class VkShader implements Shader {
    private final VulkanGraphicsPipeline pipeline;

    public VkShader(@NotNull VulkanGraphicsPipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public void close() {
        pipeline.close();
    }
}

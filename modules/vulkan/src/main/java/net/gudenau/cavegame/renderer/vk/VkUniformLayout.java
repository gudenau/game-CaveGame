package net.gudenau.cavegame.renderer.vk;

import net.gudenau.cavegame.renderer.shader.ShaderMeta;
import net.gudenau.cavegame.renderer.shader.Uniform;
import net.gudenau.cavegame.renderer.shader.UniformLayout;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class VkUniformLayout implements UniformLayout {
    private final List<Uniform> uniforms;
    private final VkUniform ubo;

    public VkUniformLayout(@NotNull VulkanShaderModule module, @NotNull Map<String, ShaderMeta.Uniform> metadata) {
        Set<String> missing = new HashSet<>();
        List<VkUniform> uniforms = new ArrayList<>();

        VkUniform ubo = null;

        for(var uniform : module.uniforms()) {
            var name = uniform.name();
            var meta = metadata.get(name);
            if(meta == null) {
                missing.add(name);
                continue;
            }

            var value = new VkUniform(uniform, meta);
            var usage = value.usage();
            if(usage != null) {
                switch(usage) {
                    case UBO -> ubo = value;
                }
            }
            uniforms.add(value);
        }

        if(!missing.isEmpty()) {
            throw new RuntimeException("Missing uniforms: " + String.join(", ", missing));
        }

        this.uniforms = Collections.unmodifiableList(uniforms);

        this.ubo = ubo;
    }

    @Override
    public List<Uniform> uniforms() {
        return uniforms;
    }

    @Override
    public Uniform ubo() {
        return ubo;
    }
}

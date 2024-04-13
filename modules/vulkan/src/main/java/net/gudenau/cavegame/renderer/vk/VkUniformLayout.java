package net.gudenau.cavegame.renderer.vk;

import net.gudenau.cavegame.renderer.shader.ShaderMeta;
import net.gudenau.cavegame.renderer.shader.Uniform;
import net.gudenau.cavegame.renderer.shader.UniformLayout;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class VkUniformLayout implements UniformLayout {
    private final List<VkUniform> uniforms;
    private final VkUniform model;
    private final VkUniform view;
    private final VkUniform projection;

    public VkUniformLayout(@NotNull VulkanShaderModule module, @NotNull Map<String, ShaderMeta.Uniform> metadata) {
        Set<String> missing = new HashSet<>();
        List<VkUniform> uniforms = new ArrayList<>();

        VkUniform model = null;
        VkUniform view = null;
        VkUniform projection = null;

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
                    case MODEL -> model = value;
                    case VIEW -> view = value;
                    case PROJECTION -> projection = value;
                }
            }
            uniforms.add(value);
        }

        if(!missing.isEmpty()) {
            throw new RuntimeException("Missing uniforms: " + String.join(", ", missing));
        }

        this.uniforms = Collections.unmodifiableList(uniforms);

        this.model = model;
        this.view = view;
        this.projection = projection;
    }

    @Override
    public List<VkUniform> uniforms() {
        return uniforms;
    }

    @Override
    public VkUniform model() {
        return model;
    }

    @Override
    public VkUniform view() {
        return view;
    }

    @Override
    public VkUniform projection() {
        return projection;
    }
}

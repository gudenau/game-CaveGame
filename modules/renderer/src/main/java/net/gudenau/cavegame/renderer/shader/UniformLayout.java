package net.gudenau.cavegame.renderer.shader;

import java.util.List;

public interface UniformLayout {
    List<Uniform> uniforms();

    Uniform ubo();
}

package net.gudenau.cavegame.renderer.shader;

import java.util.List;

public interface UniformLayout {
    List<? extends Uniform> uniforms();

    Uniform model();
    Uniform view();
    Uniform projection();
}

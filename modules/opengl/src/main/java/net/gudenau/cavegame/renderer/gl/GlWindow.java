package net.gudenau.cavegame.renderer.gl;

import net.gudenau.cavegame.renderer.Window;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;

public final class GlWindow extends GlContext implements Window {
    public GlWindow(@Nullable GlContext parent, @NotNull String title, int width, int height) {
        super(parent, title, width, height);
    }

    @Override
    public void flip() {
        glfwSwapBuffers(handle());
    }
}

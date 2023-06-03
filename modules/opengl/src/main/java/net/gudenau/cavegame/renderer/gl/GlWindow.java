package net.gudenau.cavegame.renderer.gl;

import net.gudenau.cavegame.renderer.GlfwUtils;
import net.gudenau.cavegame.renderer.Window;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

import java.lang.ref.PhantomReference;

import static org.lwjgl.glfw.GLFW.*;

public final class GlWindow extends GlContext implements Window {
    private boolean visible = false;

    public GlWindow(@Nullable GlContext parent, @NotNull String title, int width, int height) {
        super(parent, title, width, height);
    }

    @Override
    public void visible(boolean visible) {
        if(this.visible != visible) {
            this.visible = visible;
            GlfwUtils.invokeLater(() -> {
                if(visible) {
                    glfwShowWindow(context);
                } else {
                    glfwHideWindow(context);
                }
            });
        }
    }

    @Override
    public boolean visible() {
        return visible;
    }

    @Override
    public boolean closeRequested() {
        return glfwWindowShouldClose(context);
    }

    @Override
    public void flip() {
        glfwSwapBuffers(context);
    }

    @Override
    public void position(int x, int y) {
        GlfwUtils.invokeLater(() -> glfwSetWindowPos(context, x, y));
    }

    @Override
    @NotNull
    public Size size() {
        return GlfwUtils.invokeAndWait(() -> {
            try (var stack = MemoryStack.stackPush()) {
                var width = stack.ints(0);
                var height = stack.ints(0);
                glfwGetWindowSize(context, width, height);
                return new Size(width.get(0), height.get(0));
            }
        });
    }
}

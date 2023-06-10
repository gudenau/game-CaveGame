package net.gudenau.cavegame.renderer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

import java.util.Objects;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public abstract class GlfwWindow implements Window {
    private final long handle;

    private boolean visible = false;

    protected GlfwWindow(@NotNull String title, int width, int height, @Nullable Object user) {
        Objects.requireNonNull(title, "title can't be null");

        handle = GlfwUtils.invokeAndWait(() -> createWindow(title, width, height, user));

        if(handle == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }
    }

    protected abstract long createWindow(String title, int width, int height, @Nullable Object user);

    @Override
    public final void visible(boolean visible) {
        if(this.visible == visible) {
            return;
        }

        this.visible = visible;
        GlfwUtils.invokeLater(() -> {
            if(visible) {
                glfwShowWindow(handle);
            } else {
                glfwHideWindow(handle);
            }
        });
    }

    @Override
    public final boolean visible() {
        return visible;
    }

    @Override
    public final boolean closeRequested() {
        return glfwWindowShouldClose(handle);
    }

    @Override
    public final void position(int x, int y) {
        GlfwUtils.invokeLater(() -> glfwSetWindowPos(handle, x, y));
    }

    @Override
    @NotNull
    public final Size size() {
        return GlfwUtils.invokeAndWait(() -> {
            try (var stack = MemoryStack.stackPush()) {
                var width = stack.ints(0);
                var height = stack.ints(0);
                glfwGetWindowSize(handle, width, height);
                return new Size(width.get(0), height.get(0));
            }
        });
    }

    public final long handle() {
        return handle;
    }

    @NotNull
    public final Size framebufferSize() {
        return GlfwUtils.invokeAndWait(() -> {
            try (var stack = MemoryStack.stackPush()) {
                var width = stack.ints(0);
                var height = stack.ints(0);
                glfwGetFramebufferSize(handle, width, height);
                return new Size(width.get(0), height.get(0));
            }
        });
    }

    @Override
    public void close() {
        GlfwUtils.invokeLater(() -> glfwDestroyWindow(handle));
    }
}

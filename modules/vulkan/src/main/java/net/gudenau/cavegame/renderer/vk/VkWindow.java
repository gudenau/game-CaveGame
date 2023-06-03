package net.gudenau.cavegame.renderer.vk;

import net.gudenau.cavegame.renderer.GlfwUtils;
import net.gudenau.cavegame.renderer.Window;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class VkWindow implements Window {
    private final long handle;

    private boolean visible = false;

    public VkWindow(@NotNull String title, int width, int height) {
        handle = GlfwUtils.invokeAndWait(() -> {
            glfwDefaultWindowHints();

            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
            glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

            glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);

            //TODO Remove VK string
            return glfwCreateWindow(width, height, title + " (VK)", NULL, NULL);
        });

        if(handle == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }
    }

    @Override
    public void visible(boolean visible) {
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
    public boolean visible() {
        return visible;
    }

    @Override
    public boolean closeRequested() {
        return glfwWindowShouldClose(handle);
    }

    @Override
    public void bind() {}

    @Override
    public void release() {}

    @Override
    public void flip() {

    }

    @Override
    public void close() {
        GlfwUtils.invokeLater(() -> glfwDestroyWindow(handle));
    }

    @Override
    public void position(int x, int y) {
        GlfwUtils.invokeLater(() -> glfwSetWindowPos(handle, x, y));
    }

    @Override
    @NotNull
    public Size size() {
        return GlfwUtils.invokeAndWait(() -> {
            try (var stack = MemoryStack.stackPush()) {
                var width = stack.ints(0);
                var height = stack.ints(0);
                glfwGetWindowSize(handle, width, height);
                return new Size(width.get(0), height.get(0));
            }
        });
    }

    public long handle() {
        return handle;
    }

    @NotNull
    public Size framebufferSize() {
        return GlfwUtils.invokeAndWait(() -> {
            try (var stack = MemoryStack.stackPush()) {
                var width = stack.ints(0);
                var height = stack.ints(0);
                glfwGetFramebufferSize(handle, width, height);
                return new Size(width.get(0), height.get(0));
            }
        });
    }
}

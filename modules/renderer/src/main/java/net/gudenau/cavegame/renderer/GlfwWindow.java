package net.gudenau.cavegame.renderer;

import net.gudenau.cavegame.renderer.screen.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.system.MemoryStack;

import java.util.Objects;
import java.util.Optional;
import java.util.Stack;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public abstract class GlfwWindow implements Window {
    private boolean visible = false;
    private final GLFWFramebufferSizeCallback glfwResizeCallback;
    @Nullable
    private ResizeCallback resizeCallback = null;
    private final Stack<Screen> screenStack = new Stack<>();

    private final long handle;

    protected GlfwWindow(@NotNull String title, int width, int height, @Nullable Object user) {
        Objects.requireNonNull(title, "title can't be null");

        glfwResizeCallback = GLFWFramebufferSizeCallback.create(this::resizeCallback);

        handle = GlfwUtils.invokeAndWait(() -> {
            var result = createWindow(title, width, height, user);
            if(result != NULL) {
                glfwSetFramebufferSizeCallback(result, glfwResizeCallback);
            }
            return result;
        });

        if(handle == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }
    }

    private void resizeCallback(long handle, int width, int height) {
        synchronized (this) {
            if (this.handle == handle && resizeCallback != null) {
                resizeCallback.invoke(this, width, height);
            }
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

    @Override
    public void resizeCallback(@Nullable ResizeCallback callback) {
        synchronized (this) {
            this.resizeCallback = callback;
        }
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
    public void pushScreen(@NotNull Screen screen) {
        screenStack.push(screen);
    }

    @Override
    @NotNull
    public Screen popScreen(boolean close) {
        var screen = screenStack.pop();
        if(close) {
            screen.close();
        }
        return screen;
    }

    @Override
    @NotNull
    public Optional<Screen> currentScreen() {
        return screenStack.isEmpty() ? Optional.empty() : Optional.of(screenStack.peek());
    }

    @Override
    public void close() {
        GlfwUtils.invokeAndWait(() -> {
            //noinspection resource
            glfwSetFramebufferSizeCallback(handle, null);
            glfwDestroyWindow(handle);
            glfwResizeCallback.free();
        });
    }
}

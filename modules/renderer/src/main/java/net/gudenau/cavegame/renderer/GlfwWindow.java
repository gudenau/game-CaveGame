package net.gudenau.cavegame.renderer;

import net.gudenau.cavegame.renderer.screen.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.system.MemoryStack;

import java.util.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public abstract class GlfwWindow implements Window {
    private boolean visible = false;
    private final GLFWFramebufferSizeCallback glfwResizeCallback;
    @NotNull
    private SequencedSet<ResizeCallback> resizeCallbacks = new LinkedHashSet<>();
    private final Stack<Screen> screenStack = new Stack<>();

    private int framebufferWidth;
    private int framebufferHeight;

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

        var size = GlfwUtils.invokeAndWait(() -> {
            try (var stack = MemoryStack.stackPush()) {
                var fbWidth = stack.ints(0);
                var fbHeight = stack.ints(0);
                glfwGetFramebufferSize(handle, fbWidth, fbHeight);
                return new Size(fbWidth.get(0), fbHeight.get(0));
            }
        });
        this.framebufferWidth = size.width();
        this.framebufferHeight = size.height();

        if(handle == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }
    }

    private void resizeCallback(long handle, int width, int height) {
        synchronized (this) {
            this.framebufferWidth = width;
            this.framebufferHeight = height;

            screenStack.forEach((screen) -> screen.setSize(width, height));

            if (this.handle == handle) {
                resizeCallbacks.forEach((callback) -> callback.invoke(this, width, height));
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
        return framebufferSize();

        /* TODO, check to make sure this is correct.
        return GlfwUtils.invokeAndWait(() -> {
            try (var stack = MemoryStack.stackPush()) {
                var width = stack.ints(0);
                var height = stack.ints(0);
                glfwGetWindowSize(handle, width, height);
                return new Size(width.get(0), height.get(0));
            }
        });
         */
    }

    @Override
    public ResizeCallback addResizeCallback(@NotNull ResizeCallback callback) {
        synchronized (this) {
            this.resizeCallbacks.add(callback);
        }
        return callback;
    }

    @Override
    public void removeResizeCallback(@NotNull ResizeCallback callback) {
        synchronized (this) {
            this.resizeCallbacks.remove(callback);
        }
    }

    public final long handle() {
        return handle;
    }

    @NotNull
    public final Size framebufferSize() {
        synchronized (this) {
            return new Size(framebufferWidth, framebufferHeight);
        }
    }

    @Override
    public void pushScreen(@NotNull Screen screen) {
        var size = framebufferSize();
        screen.setSize(size.width(), size.height());

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

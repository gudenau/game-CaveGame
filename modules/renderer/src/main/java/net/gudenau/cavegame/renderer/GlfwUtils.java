package net.gudenau.cavegame.renderer;

import net.gudenau.cavegame.config.Config;
import net.gudenau.cavegame.logger.Logger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class GlfwUtils {
    private static final Logger LOGGER = Logger.forName("GlfwUtils");
    private static final BlockingQueue<Runnable> JOBS = new LinkedBlockingQueue<>();

    private static volatile boolean running = true;

    private GlfwUtils() {
        throw new AssertionError();
    }

    public static void handoverMain(@NotNull Runnable newMain) {
        LOGGER.debug("Initializing GLFW...");
        //noinspection resource
        glfwSetErrorCallback((error, description) -> LOGGER.error(glfwError(error, description)));

        var glfwThread = Thread.currentThread();
        glfwThread.setName("GLFW Worker");
        if(glfwThread.threadId() != 1) {
            throw new IllegalStateException("GLFW can't be handed over outside of the original main thread!");
        }

        if(Platform.get() == Platform.LINUX && !Config.FORCE_X.get()) {
            if(glfwPlatformSupported(GLFW_PLATFORM_WAYLAND)) {
                glfwInitHint(GLFW_PLATFORM, GLFW_PLATFORM_WAYLAND);
            }
        }

        if(!glfwInit()) {
            throw new RuntimeException("Failed to initialize GLFW: " + glfwError());
        }

        invokeLater(() -> new Thread(() -> {
            try {
                newMain.run();
            } finally {
                running = false;
                glfwThread.interrupt();
            }
        }, "main").start());

        while(running) {
            Runnable job;
            try {
                job = JOBS.take();
            } catch (InterruptedException e) {
                if(running) {
                    LOGGER.warn("GLFW thread interrupted while still running, ignoring...", e);
                }
                continue;
            }
            try {
                job.run();
            } catch (Throwable e) {
                LOGGER.error("GLFW task threw an unexpected exception", e);
            }
        }

        LOGGER.debug("Terminating GLFW...");
        glfwTerminate();
        //noinspection resource,DataFlowIssue
        glfwSetErrorCallback(null).free();
    }

    public static CompletableFuture<Void> invokeLater(@NotNull Runnable task) {
        Objects.requireNonNull(task, "task can't be null");
        return CompletableFuture.runAsync(task, JOBS::add);
    }

    public static <T> CompletableFuture<T> invokeLater(@NotNull Supplier<T> task) {
        Objects.requireNonNull(task, "task can't be null");
        return CompletableFuture.supplyAsync(task, JOBS::add);
    }

    public static void invokeAndWait(@NotNull Runnable task) {
        invokeLater(task).join();
    }

    public static <T> T invokeAndWait(@NotNull Supplier<T> task) {
        return invokeLater(task).join();
    }

    @NotNull
    private static String glfwError(int error, long description) {
        if(description != NULL) {
            return MemoryUtil.memUTF8(description) + " (" + error + ")";
        } else {
            return Integer.toString(error);
        }
    }

    @NotNull
    private static String glfwError() {
        try(var stack = MemoryStack.stackPush()) {
            var description = stack.callocPointer(1);
            var error = glfwGetError(description);
            return glfwError(error, description.get(0));
        }
    }

    public static void poll() {
        invokeAndWait(GLFW::glfwPollEvents);
    }
}

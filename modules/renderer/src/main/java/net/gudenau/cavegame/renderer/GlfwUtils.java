package net.gudenau.cavegame.renderer;

import net.gudenau.cavegame.config.Config;
import net.gudenau.cavegame.logger.Logger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.ValueLayout;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class GlfwUtils {
    private static final Logger LOGGER = Logger.forName("GlfwUtils");
    private static final BlockingQueue<Runnable> JOBS = new LinkedBlockingQueue<>();

    private static volatile boolean running = true;

    private GlfwUtils() {
        throw new AssertionError();
    }

    public static void handoverMain(@NotNull Runnable newMain) {
        try(var stack = MemoryStack.stackPush()) {
            var major = stack.ints(0);
            var minor = stack.ints(0);
            var revision = stack.ints(0);
            glfwGetVersion(major, minor, revision);
            LOGGER.debug("Initializing GLFW v" + major.get(0) + '.' + minor.get(0) + '.' + revision.get(0) + "...");
        }

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
                JOBS.add(() -> {});
            }
        }, "main").start());

        while(running) {
            Runnable job;
            try {
                job = JOBS.take();
            } catch (InterruptedException e) {
                LOGGER.error("GLFW thread interrupted while running a job", e);
                continue;
            }
            try {
                job.run();
            } catch (Throwable e) {
                LOGGER.error("GLFW task threw an unexpected exception", e);
            }
        }

        while(!JOBS.isEmpty()) {
            try {
                JOBS.take().run();
            } catch(Throwable e) {
                LOGGER.error("Failed to execute job while flushing", e);
            }
        }

        LOGGER.debug("Terminating GLFW...");
        glfwTerminate();
        //noinspection resource,DataFlowIssue
        glfwSetErrorCallback(null).free();
    }

    public static CompletableFuture<Void> invokeLater(@NotNull Runnable task) {
        Objects.requireNonNull(task, "task can't be null");
        if(!running) {
            throw new IllegalStateException("GLFW is shutting down");
        }
        return CompletableFuture.runAsync(task, JOBS::add);
    }

    public static <T> CompletableFuture<T> invokeLater(@NotNull Supplier<T> task) {
        Objects.requireNonNull(task, "task can't be null");
        if(!running) {
            throw new IllegalStateException("GLFW is shutting down");
        }
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
        var errorName = switch(error) {
            case GLFW_NO_ERROR -> "GLFW_NO_ERROR";
            case GLFW_NOT_INITIALIZED -> "GLFW_NOT_INITIALIZED";
            case GLFW_NO_CURRENT_CONTEXT -> "GLFW_NO_CURRENT_CONTEXT";
            case GLFW_INVALID_ENUM -> "GLFW_INVALID_ENUM";
            case GLFW_INVALID_VALUE -> "GLFW_INVALID_VALUE";
            case GLFW_OUT_OF_MEMORY -> "GLFW_OUT_OF_MEMORY";
            case GLFW_API_UNAVAILABLE -> "GLFW_API_UNAVAILABLE";
            case GLFW_VERSION_UNAVAILABLE -> "GLFW_VERSION_UNAVAILABLE";
            case GLFW_PLATFORM_ERROR -> "GLFW_PLATFORM_ERROR";
            case GLFW_FORMAT_UNAVAILABLE -> "GLFW_FORMAT_UNAVAILABLE";
            case GLFW_NO_WINDOW_CONTEXT -> "GLFW_NO_WINDOW_CONTEXT";
            case GLFW_CURSOR_UNAVAILABLE -> "GLFW_CURSOR_UNAVAILABLE";
            case GLFW_FEATURE_UNAVAILABLE -> "GLFW_FEATURE_UNAVAILABLE";
            case GLFW_FEATURE_UNIMPLEMENTED -> "GLFW_FEATURE_UNIMPLEMENTED";
            case GLFW_PLATFORM_UNAVAILABLE -> "GLFW_PLATFORM_UNAVAILABLE";
            default -> "UNKNOWN (" + error + ')';
        };

        if(description != NULL) {
            return errorName + ": " + MemoryUtil.memUTF8(description);
        } else {
            return errorName;
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

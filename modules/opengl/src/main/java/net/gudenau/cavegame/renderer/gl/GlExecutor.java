package net.gudenau.cavegame.renderer.gl;

import net.gudenau.cavegame.util.ExclusiveLock;
import net.gudenau.cavegame.util.ThreadPool;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class GlExecutor implements AutoCloseable {
    @NotNull
    private final ExclusiveLock lock = new ExclusiveLock();
    @NotNull
    private final Stack<Context> contextStack = new Stack<>();
    @NotNull
    private final GlContext primordialContext;

    GlExecutor(@NotNull GlContext primordialContext) {
        this.primordialContext = primordialContext;
    }

    @NotNull
    private Context aquireContext() {
        var context = lock.lock(() ->
            contextStack.isEmpty() ?
                new Context(primordialContext) :
                contextStack.pop()
        );
        context.bind();
        return context;
    }

    @SuppressWarnings("resource")
    private void releaseContext(@NotNull Context context) {
        context.release();
        lock.lock(() -> contextStack.push(context));
    }

    @NotNull
    public CompletableFuture<Void> schedule(@NotNull Consumer<GlState> task) {
        return ThreadPool.future(() -> run(task));
    }

    @NotNull
    public <T> CompletableFuture<T> schedule(@NotNull Function<GlState, T> task) {
        return ThreadPool.future(() -> run(task));
    }

    private void run(@NotNull Consumer<GlState> task) {
        run((Function<GlState, Void>)(state) -> {
            task.accept(state);
            return null;
        });
    }

    public <T> T run(@NotNull Function<GlState, T> action) {
        Context context;
        var handle = glfwGetCurrentContext();
        if(handle == NULL) {
            context = aquireContext();
            handle = context.handle();
        } else {
            context = null;
        }
        try {
            return action.apply(GlState.get(handle));
        } finally {
            if(context != null) {
                releaseContext(context);
            }
        }
    }

    @Override
    public void close() {
        lock.lock(() -> {
            contextStack.forEach(GlContext::close);
            contextStack.clear();
        });
    }

    static final class Context extends GlContext {
        private Context(GlContext parent) {
            super(parent, "CaveGame worker", 1, 1);
        }
    }
}

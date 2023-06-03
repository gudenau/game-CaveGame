package net.gudenau.cavegame.renderer.gl;

import net.gudenau.cavegame.util.ExclusiveLock;
import org.jetbrains.annotations.NotNull;

import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public final class GlExecutor implements AutoCloseable {
    @NotNull
    private final ExclusiveLock lock = new ExclusiveLock();
    @NotNull
    private final Stack<Context> context = new Stack<>();
    @NotNull
    private final GlContext primordialContext;
    @NotNull
    private final Executor executor;

    @SuppressWarnings("resource") // Idea is being too sensitive here
    GlExecutor(@NotNull GlContext primordialContext) {
        this.primordialContext = primordialContext;

        //TODO Make this not garbage, requires virtual thread pinning.
        executor = (task) -> {
            var context = lock.lock(() -> this.context.isEmpty() ? new Context(primordialContext) : this.context.pop());
            try {
                context.bind();
                task.run();
            } finally {
                context.release();
                lock.lock(() -> this.context.push(context));
            }
        };
    }

    @NotNull
    public CompletableFuture<Void> schedule(@NotNull Runnable task) {
        return CompletableFuture.runAsync(task, executor);
    }

    @NotNull
    public <T> CompletableFuture<T> schedule(@NotNull Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executor);
    }

    @Override
    public void close() {
        lock.lock(() -> {
            context.forEach(GlContext::close);
            context.clear();
        });
    }

    static final class Context extends GlContext {
        private Context(GlContext parent) {
            super(parent, "CaveGame worker", 1, 1);
        }
    }
}

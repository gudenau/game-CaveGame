package net.gudenau.cavegame.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

/**
 * A helper to use virtual threads for parallel tasks.
 */
public final class ThreadPool {
    /**
     * The factory used to create threads.
     */
    private static final ThreadFactory FACTORY = Thread.ofVirtual()
        //.allowSetThreadLocals(false)
        //.inheritInheritableThreadLocals(false)
        .name("VirtualPool-", 0)
        .factory();

    /**
     * The executor to create and run threads.
     */
    private static final Executor EXECUTOR = (job) -> FACTORY.newThread(job).start();

    /**
     * Schedules a task to be executed some time in the future in a virtual thread.
     *
     * @param job The job to schedule
     * @return The future of this job
     * @param <T> The return type of the job
     */
    public static <T> CompletableFuture<T> future(Supplier<T> job) {
        return CompletableFuture.supplyAsync(job, EXECUTOR);
    }

    /**
     * Schedules a task to be executed some time in the future in a virtual thread.
     *
     * @param job The job to schedule
     */
    public static CompletableFuture<Void> future(Runnable job) {
        return CompletableFuture.runAsync(job, EXECUTOR);
    }
}

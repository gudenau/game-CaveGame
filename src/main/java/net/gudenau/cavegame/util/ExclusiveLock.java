package net.gudenau.cavegame.util;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * A wrapper for an exclusive {@link Lock} implementation.
 */
public final class ExclusiveLock {
    /**
     * The exclusive lock.
     */
    private final Lock lock = new ReentrantLock();

    /**
     * Acquires the exclusive lock; blocking if required, performs the given job and releases the lock.
     *
     * @param task The task to execute
     */
    public void lock(@NotNull Runnable task) {
        lock.lock();
        try {
            task.run();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Acquires the exclusive lock; blocking if required, performs the given job and releases the lock.
     *
     * @param task The task to execute
     * @return The result of the task
     */
    public <T> T lock(@NotNull Supplier<T> task) {
        lock.lock();
        try {
            return task.get();
        } finally {
            lock.unlock();
        }
    }
}

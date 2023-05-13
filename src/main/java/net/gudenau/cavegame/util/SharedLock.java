package net.gudenau.cavegame.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * A shared lock helper.
 */
public final class SharedLock {
    /**
     * The shared lock instance.
     */
    @NotNull
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * The shared lock half of the shared lock.
     */
    @NotNull
    private final Lock readLock = lock.readLock();

    /**
     * The exclusive lock half of the shared lock.
     */
    @NotNull
    private final Lock writeLock = lock.writeLock();

    /**
     * Acquires the shared lock; blocking if required, performs the given job and releases the lock.
     *
     * @param task The task to execute
     */
    public void read(@NotNull Runnable task) {
        readLock.lock();
        try {
            task.run();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Acquires the shared lock; blocking if required, performs the given job and releases the lock.
     *
     * @param task The task to execute
     * @return The result of the task
     */
    public <T> T read(@NotNull Supplier<T> task) {
        readLock.lock();
        try {
            return task.get();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Acquires the exclusive lock; blocking if required, performs the given job and releases the lock.
     *
     * @param task The task to execute
     */
    public void write(@NotNull Runnable task) {
        writeLock.lock();
        try {
            task.run();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Acquires the exclusive lock; blocking if required, performs the given job and releases the lock.
     *
     * @param task The task to execute
     * @return The result of the task
     */
    public <T> T write(@NotNull Supplier<T> task) {
        writeLock.lock();
        try {
            return task.get();
        } finally {
            writeLock.unlock();
        }
    }
}

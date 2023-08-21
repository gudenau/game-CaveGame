package net.gudenau.cavegame.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    /**
     * Calls the read action under the read lock and returns its result if non-null. If the result was null the write
     * lock is acquired and read will be called again and the result returned if non-null. If the result is still null
     * the write action will be called and its value returned.
     * <p>
     * The read action is called twice because in the time between releasing the read lock and acquiring the write lock
     * the state may have changed, requiring the state to be re-evaluated.
     *
     * @param read The action to be preformed with a read lock
     * @param write The action to be preformed with a write lock
     * @return The result of the actions
     * @param <T> The generic result of the actions
     */
    @NotNull
    public <T> T readWrite(@NotNull Supplier<@Nullable T> read, @NotNull Supplier<@NotNull T> write) {
        T value;
        readLock.lock();
        try {
            value = read.get();
        } finally {
            readLock.unlock();
        }
        if(value != null) {
            return value;
        }

        writeLock.lock();
        try {
            value = read.get();
            return value == null ? write.get() : value;
        } finally {
            writeLock.lock();
        }
    }
}

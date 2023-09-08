package net.gudenau.cavegame.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * A helper to make dealing with native resources a touch easier. Closes {@link AutoCloseable} instances in reverse
 * chronological order.
 */
public final class Closer implements AutoCloseable {
    private final ExclusiveLock lock = new ExclusiveLock();
    private final SequencedSet<AutoCloseable> elements = new LinkedHashSet<>();
    private volatile boolean closed = false;

    private void ensureOpen() {
        if(closed) {
            throw new RuntimeException("Can not manipulate a closed Closer");
        }
    }

    /**
     * Adds an item to be closed later.
     *
     * @param element The item to be closed
     */
    @Contract("_ -> param1")
    @NotNull
    public <T extends AutoCloseable> T add(@NotNull T element) {
        lock.lock(() -> {
            ensureOpen();
            elements.add(element);
        });
        return element;
    }

    /**
     * Removes and closes an element without closing all other elements.
     *
     * @param element The item to be closed
     */
    @Contract("_ -> param1")
    @NotNull
    public <T extends AutoCloseable> T close(@NotNull T element) {
        lock.lock(() -> {
            ensureOpen();
            elements.remove(element);
        });
        try {
            element.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to close element " + element, e);
        }
        return element;
    }


    @Override
    public void close() {
        lock.lock(() -> {
            if(closed) {
                return;
            }
            closed = true;

            while(!elements.isEmpty()) {
                var element = elements.removeLast();
                try {
                    element.close();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to close element " + element, e);
                }
            }
        });
    }
}

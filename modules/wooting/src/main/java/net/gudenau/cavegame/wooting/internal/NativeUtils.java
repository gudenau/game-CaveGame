package net.gudenau.cavegame.wooting.internal;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.Buffer;
import java.nio.file.Path;

/**
 * Some random native-related helpers.
 */
public final class NativeUtils {
    private NativeUtils() {
        throw new AssertionError();
    }

    /**
     * Ensures that a MemorySegment is both non-null in Java land and C land.
     *
     * @param segment The segment to check
     * @param message The message to pass to the exception
     * @return The passed segment
     */
    @Contract("_, _ -> param1")
    @NotNull
    @SuppressWarnings("ConstantValue")
    public static MemorySegment ensureNonNull(@NotNull MemorySegment segment, @NotNull String message) {
        if(segment == null || segment.equals(MemorySegment.NULL)) {
            throw new NullPointerException(message);
        }
        return segment;
    }

    /**
     * Gets a String or null from the passed memory segment.
     *
     * @param pointer The segment to read
     * @return The string or null
     */
    @Nullable
    public static String string(@NotNull MemorySegment pointer) {
        //TODO Find a better way, this is hacky.
        if (pointer.equals(MemorySegment.NULL)) {
            return null;
        }
        return pointer.reinterpret(Long.MAX_VALUE).getString(0);
    }

    /**
     * Gets the {@link MemorySegment} from the provided {@link Buffer}. The buffer must be direct and be read-write.
     *
     * @param buffer The buffer to get
     * @return The resulting segment
     */
    @NotNull
    public static MemorySegment segment(Buffer buffer) {
        if(!buffer.isDirect()) {
            throw new IllegalArgumentException(buffer.getClass().getSimpleName() + " must be direct");
        }
        if(buffer.isReadOnly()) {
            throw new IllegalArgumentException(buffer.getClass().getSimpleName() + " must be writeable");
        }
        return MemorySegment.ofBuffer(buffer);
    }
}

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
        var segment = MemorySegment.ofAddress(pointer.address(), Long.MAX_VALUE, pointer.scope());
        return segment.getUtf8String(0);
    }

    /**
     * The lookup to use when finding symbols to the Wooting SDK.
     */
    private static final SymbolLookup LOOKUP = SymbolLookup.libraryLookup(Path.of("/home/gudenau/projects/java/games/CaveGame/external/wooting-analog-sdk/wrapper/libwooting_analog_wrapper.so"), SegmentScope.global());
    /**
     * The linker to use when interfacing with the Wooting SDK.
     */
    private static final Linker LINKER = Linker.nativeLinker();

    /**
     * Creates a {@link MethodHandle} bound to a function in the Wooting SDK.
     *
     * @param name The name of the function symbol
     * @param result The return type of the function or null for void
     * @param args The arguments of the function
     * @return The created {@link MethodHandle}
     */
    @NotNull
    public static MethodHandle function(@NotNull String name, @Nullable MemoryLayout result, @NotNull MemoryLayout @NotNull ... args) {
        var symbol = LOOKUP.find(name).orElseThrow(() -> new RuntimeException("Failed to find symbol " + name));
        return LINKER.downcallHandle(symbol, result == null ? FunctionDescriptor.ofVoid(args) : FunctionDescriptor.of(result, args));
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

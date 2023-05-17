package net.gudenau.cavegame.wooting;

import net.gudenau.cavegame.wooting.internal.NativeUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.foreign.MemorySegment;

/**
 * An opaque data type that is used by the Wooting SDK.
 *
 * @param value The native pointer
 */
public record DeviceInfo(@NotNull MemorySegment value) {
    public DeviceInfo {
        NativeUtils.ensureNonNull(value, "value can't be null");
    }
}

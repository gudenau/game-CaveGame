package net.gudenau.cavegame.util;

import org.jetbrains.annotations.Range;

/**
 * Math utilities that the built-in math class doesn't have.
 */
public final class MathUtils {
    private MathUtils() {
        throw new AssertionError();
    }

    /**
     * Performs a saturating add on two positive longs.
     *
     * @param x The X value
     * @param y The Y value
     * @return The result
     */
    public static long saturatingAdd(@Range(from = 0, to = Long.MAX_VALUE) long x, @Range(from = 0, to = Long.MAX_VALUE) long y) {
        long r = x + y;
        // From Math.addExact(long, long)long
        if (((x ^ r) & (y ^ r)) < 0) {
            return Long.MAX_VALUE;
        }
        return r;
    }
}

package net.gudenau.cavegame.gui;

import org.jetbrains.annotations.NotNull;

/// Cardinal directions for use in GUIs.
public enum Direction {
    RIGHT(Axis.X, Sign.POSITIVE),
    LEFT(Axis.X, Sign.NEGATIVE),
    UP(Axis.Y, Sign.POSITIVE),
    DOWN(Axis.Y, Sign.NEGATIVE),
    ;

    /// Returns the {@link Direction} constant that corresponds to the provided {@link Axis} and {@link Sign}. For
    /// example, {@link Axis#X X} and {@link Sign#POSITIVE} will return {@link Direction#RIGHT}.
    ///
    /// @param axis The {@link Axis} component
    /// @param sign The {@link Sign} component
    /// @return The corresponding {@link Direction}
    @NotNull
    public static Direction of(@NotNull Axis axis, @NotNull Sign sign) {
        return switch(axis) {
            case X -> sign == Sign.POSITIVE ? RIGHT : LEFT;
            case Y -> sign == Sign.POSITIVE ? UP : DOWN;
        };
    }

    @NotNull
    private final Axis axis;
    @NotNull
    private final Sign sign;

    Direction(@NotNull Axis axis, @NotNull Sign sign) {
        this.axis = axis;
        this.sign = sign;
    }

    /// Returns the {@link Direction} that points in the opposite direction of this {@link Direction}.
    /// @return The opposite {@link Direction}
    @NotNull
    public Direction opposite() {
        return of(axis, sign.invert());
    }

    /// Gets the {@link Axis} of this {@link Direction}.
    /// @return The {@link Axis}
    @NotNull
    public Axis axis() {
        return axis;
    }

    /// Gets the {@link Sign} of this {@link Direction}.
    /// @return The {@link Sign}
    @NotNull
    public Sign sign() {
        return sign;
    }

    /// The different axis that a {@link Direction} can point. For example, {@link Direction#RIGHT} is {@link Axis#X}.
    public enum Axis {
        X,
        Y,
    }

    /// The different signs that a {@link Direction} can point. For example, {@link Direction#RIGHT} is
    /// {@link Sign#POSITIVE}.
    public enum Sign {
        POSITIVE,
        NEGATIVE,
        ;

        /// Returns the opposite {@link Sign} of this {@link Sign}.
        /// @return The opposite {@link Sign}
        @NotNull
        public Sign invert() {
            return this == POSITIVE ? NEGATIVE : POSITIVE;
        }
    }
}

package net.gudenau.cavegame.gui;

import org.jetbrains.annotations.NotNull;

public enum Direction {
    RIGHT(Axis.X, Sign.POSITIVE),
    LEFT(Axis.X, Sign.NEGATIVE),
    UP(Axis.Y, Sign.POSITIVE),
    DOWN(Axis.Y, Sign.NEGATIVE),
    ;

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

    @NotNull
    public Direction opposite() {
        return of(axis, sign.invert());
    }

    @NotNull
    public Axis axis() {
        return axis;
    }

    @NotNull
    public Sign sign() {
        return sign;
    }

    public enum Axis {
        X,
        Y,
    }

    public enum Sign {
        POSITIVE,
        NEGATIVE,
        ;

        @NotNull
        public Sign invert() {
            return this == POSITIVE ? NEGATIVE : POSITIVE;
        }
    }
}

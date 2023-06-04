package net.gudenau.cavegame.logger;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * All valid log levels for the logging implementation.
 */
public enum LogLevel {
    /**
     * A debug message. Printed to the console in a dark gray color.
     */
    DEBUG(0x80_80_80),
    /**
     * An informative message. Printed to the console in a light gray color.
     */
    INFO(0xF0_F0_F0),
    /**
     * A warning message. Printed to the console in a yellow color.
     */
    WARN(0xF0_F0_00),
    /**
     * An error message. Printed to the console in a red color.
     */
    ERROR(0xD0_00_00),
    /**
     * A fatal message. Printed to the console in a bright red color.
     */
    FATAL(0xFF_00_00),
    ;

    /**
     * An immutable map of all levels indexed by their lowercase name.
     */
    private static final Map<String, LogLevel> LEVELS = Stream.of(values())
        .collect(Collectors.toUnmodifiableMap(
            (level) -> level.name().toLowerCase(Locale.ROOT),
            Function.identity()
        ));

    /**
     * Gets the log level by the provided lowercase name.
     *
     * @param name The name of the level
     * @return The log level
     * @throws IllegalArgumentException If the provided name did not match an enum value
     */
    @Contract(pure = true)
    @NotNull
    public static LogLevel of(@NotNull String name) {
        var level = LEVELS.get(name);
        if(level == null) {
            throw new IllegalArgumentException("Unknown LogLevel " + name);
        }
        return level;
    }

    /**
     * The color to use when printing a message at this level.
     */
    final int color;

    LogLevel(int color) {
        this.color = color;
    }
}

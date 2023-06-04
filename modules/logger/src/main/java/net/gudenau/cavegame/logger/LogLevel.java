package net.gudenau.cavegame.logger;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum LogLevel {
    DEBUG(0x80_80_80),
    INFO(0xF0_F0_F0),
    WARN(0xF0_F0_00),
    ERROR(0xD0_00_00),
    FATAL(0xFF_00_00),
    ;

    private static final Map<String, LogLevel> LEVELS = Stream.of(values())
        .collect(Collectors.toUnmodifiableMap(
            (level) -> level.name().toLowerCase(Locale.ROOT),
            Function.identity()
        ));

    @Contract(pure = true)
    @NotNull
    public static LogLevel of(@NotNull String name) {
        var level = LEVELS.get(name);
        if(level == null) {
            throw new IllegalArgumentException("Unknown LogLevel " + name);
        }
        return level;
    }

    final int color;

    LogLevel(int color) {
        this.color = color;
    }
}

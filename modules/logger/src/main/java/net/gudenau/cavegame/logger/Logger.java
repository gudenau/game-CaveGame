package net.gudenau.cavegame.logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class Logger {
    private static final String ANSI_ESCAPE = "\u001B[";
    private static final String ANSI_RESET = ANSI_ESCAPE + "0m";

    private static LogLevel LEVEL = LogLevel.DEBUG;
    private static final Map<String, Logger> LOGGERS = new HashMap<>();

    @NotNull
    private final String name;

    private Logger(@NotNull String name) {
        this.name = name;
    }

    private void print(LogLevel level, String message) {
        var red = (level.color >>> 16) & 0xFF;
        var green = (level.color >>> 8) & 0xFF;
        var blue = level.color & 0xFF;

        var prefix = ANSI_ESCAPE + "38;2;" + red + ';' + green + ';' + blue + "m[" + level.name().toLowerCase() + "][" + name + "] ";
        message = message.lines().collect(Collectors.joining(ANSI_RESET + "\n" + prefix, prefix, ANSI_RESET));

        synchronized (System.out) {
            System.out.println(message);
        }
    }

    public void log(@NotNull LogLevel level, @NotNull String message, @Nullable Throwable exception) {
        Objects.requireNonNull(level, "level can't be null");
        Objects.requireNonNull(message, "message can't be null");

        if(level.ordinal() < LEVEL.ordinal()) {
            return;
        }

        if(exception == null) {
            print(level, message);
            return;
        }

        var writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        print(level, message + '\n' + writer);
    }

    public void log(@NotNull LogLevel level, @NotNull String message) {
        log(level, message, null);
    }

    public void debug(@NotNull String message) {
        log(LogLevel.DEBUG, message, null);
    }

    public void debug(@NotNull String message, @Nullable Throwable exception) {
        log(LogLevel.DEBUG, message, exception);
    }

    public void info(@NotNull String message) {
        log(LogLevel.INFO, message, null);
    }

    public void info(@NotNull String message, @Nullable Throwable exception) {
        log(LogLevel.INFO, message, exception);
    }

    public void warn(@NotNull String message) {
        log(LogLevel.WARN, message, null);
    }

    public void warn(@NotNull String message, @Nullable Throwable exception) {
        log(LogLevel.WARN, message, exception);
    }

    public void error(@NotNull String message) {
        log(LogLevel.ERROR, message, null);
    }

    public void error(@NotNull String message, @Nullable Throwable exception) {
        log(LogLevel.ERROR, message, exception);
    }

    public void fatal(@NotNull String message) {
        log(LogLevel.FATAL, message, null);
    }

    public void fatal(@NotNull String message, @Nullable Throwable exception) {
        log(LogLevel.FATAL, message, exception);
    }

    @NotNull
    public static Logger forName(@NotNull String name) {
        Objects.requireNonNull(name, "name can't be null");

        synchronized (LOGGERS) {
            return LOGGERS.computeIfAbsent(name, Logger::new);
        }
    }

    @NotNull
    public static Logger forClass(@NotNull Class<?> owner) {
        Objects.requireNonNull(owner, "owner can't be null");

        return forName(owner.getModule().getName() + '/' + owner.getSimpleName());
    }

    @NotNull
    public static Logger forModule(@NotNull Module module) {
        Objects.requireNonNull(module, "module can't be null");

        return forName(module.getName());
    }

    public static void level(@NotNull LogLevel level) {
        Objects.requireNonNull(level, "level can't be null");

        LEVEL = level;
    }
}

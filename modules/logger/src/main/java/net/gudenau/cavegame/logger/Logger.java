package net.gudenau.cavegame.logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The logger for CaveGame, this requires no dependencies and does exactly what we need and nothing more. (Looking at
 * you log4j).
 */
public final class Logger {
    /**
     * The ANSI escape sequence.
     */
    private static final String ANSI_ESCAPE = "\u001B[";

    /**
     * Resets ANSI colors to the default, used on new lines to prevent weird coloring issues.
     */
    private static final String ANSI_RESET = ANSI_ESCAPE + "0m";

    /**
     * The minimum log level to display.
     */
    private static LogLevel LEVEL = LogLevel.DEBUG;

    /**
     * A cache of logger instances.
     */
    private static final Map<String, Logger> LOGGERS = new HashMap<>();

    /**
     * The name of this logger.
     */
    @NotNull
    private final String name;

    /**
     * Creates a new logger instance with the provided name.
     *
     * @param name The name of the logger
     */
    private Logger(@NotNull String name) {
        this.name = name;
    }

    /**
     * Prints a message to standard output, this handles the coloration, line formatting, the log message prefixes and
     * synchronization.
     *
     * @param level The level of the message
     * @param message The message itself
     */
    private void print(@NotNull LogLevel level, @NotNull String message) {
        var red = (level.color >>> 16) & 0xFF;
        var green = (level.color >>> 8) & 0xFF;
        var blue = level.color & 0xFF;

        var prefix = ANSI_ESCAPE + "38;2;" + red + ';' + green + ';' + blue + "m[" + level.name().toLowerCase() + "][" + name + "] ";
        message = message.lines().collect(Collectors.joining(ANSI_RESET + "\n" + prefix, prefix, ANSI_RESET));

        synchronized (System.out) {
            System.out.println(message);
        }
    }

    /**
     * Logs a message to standard output if the current log level permits. The exception will be printed after the
     * message, if present.
     *
     * @param level The log level of the message
     * @param message The contents of the message
     * @param exception The exception that caused this message
     */
    public void log(@NotNull LogLevel level, @NotNull String message, @Nullable Throwable exception) {
        Objects.requireNonNull(level, "level can't be null");
        Objects.requireNonNull(message, "message can't be null");

        if(level.ordinal() < LEVEL.ordinal()) {
            return;
        }

        // A small short-circuit.
        if(exception == null) {
            print(level, message);
            return;
        }

        var writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        print(level, message + '\n' + writer);
    }

    /**
     * Logs a message to standard output if the current log level permits.
     *
     * @param level The log level of the message
     * @param message The contents of the message
     */
    public void log(@NotNull LogLevel level, @NotNull String message) {
        log(level, message, null);
    }

    /**
     * Logs a debug message to standard output if the current log level permits.
     *
     * @param message The contents of the message
     */
    public void debug(@NotNull String message) {
        log(LogLevel.DEBUG, message, null);
    }

    /**
     * Logs a debug message to standard output if the current log level permits. The exception will be printed after the
     * message, if present.
     *
     * @param message The contents of the message
     * @param exception The exception that caused this message
     */
    public void debug(@NotNull String message, @Nullable Throwable exception) {
        log(LogLevel.DEBUG, message, exception);
    }

    /**
     * Logs an informative message to standard output if the current log level permits.
     *
     * @param message The contents of the message
     */
    public void info(@NotNull String message) {
        log(LogLevel.INFO, message, null);
    }

    /**
     * Logs an informative message to standard output if the current log level permits. The exception will be printed
     * after the message, if present.
     *
     * @param message The contents of the message
     * @param exception The exception that caused this message
     */
    public void info(@NotNull String message, @Nullable Throwable exception) {
        log(LogLevel.INFO, message, exception);
    }

    /**
     * Logs a warning message to standard output if the current log level permits.
     *
     * @param message The contents of the message
     */
    public void warn(@NotNull String message) {
        log(LogLevel.WARN, message, null);
    }

    /**
     * Logs a warning message to standard output if the current log level permits. The exception will be printed after
     * the message, if present.
     *
     * @param message The contents of the message
     * @param exception The exception that caused this message
     */
    public void warn(@NotNull String message, @Nullable Throwable exception) {
        log(LogLevel.WARN, message, exception);
    }

    /**
     * Logs an error message to standard output if the current log level permits.
     *
     * @param message The contents of the message
     */
    public void error(@NotNull String message) {
        log(LogLevel.ERROR, message, null);
    }

    /**
     * Logs an error message to standard output if the current log level permits. The exception will be printed after
     * the message, if present.
     *
     * @param message The contents of the message
     * @param exception The exception that caused this message
     */
    public void error(@NotNull String message, @Nullable Throwable exception) {
        log(LogLevel.ERROR, message, exception);
    }

    /**
     * Logs a fatal message to standard output if the current log level permits.
     *
     * @param message The contents of the message
     */
    public void fatal(@NotNull String message) {
        log(LogLevel.FATAL, message, null);
    }

    /**
     * Logs a fatal message to standard output if the current log level permits. The exception will be printed after the
     * message, if present.
     *
     * @param message The contents of the message
     * @param exception The exception that caused this message
     */
    public void fatal(@NotNull String message, @Nullable Throwable exception) {
        log(LogLevel.FATAL, message, exception);
    }

    /**
     * Gets an existing logger or creates a new logger with the provided name.
     *
     * @param name The name of the logger
     * @return The logger instance
     */
    @NotNull
    public static Logger forName(@NotNull String name) {
        Objects.requireNonNull(name, "name can't be null");

        synchronized (LOGGERS) {
            return LOGGERS.computeIfAbsent(name, Logger::new);
        }
    }

    /**
     * Gets an existing logger or creates a new logger with the provided class as the name. The name is in the format
     * `module/class.name`.
     *
     * @param owner The name of the logger
     * @return The logger instance
     */
    @NotNull
    public static Logger forClass(@NotNull Class<?> owner) {
        Objects.requireNonNull(owner, "owner can't be null");

        return forName(owner.getModule().getName() + '/' + owner.getSimpleName());
    }

    /**
     * Gets an existing logger or creates a new logger with the provided module as the name. The name will be the name
     * of the module.
     *
     * @param module The name of the logger
     * @return The logger instance
     */
    @NotNull
    public static Logger forModule(@NotNull Module module) {
        Objects.requireNonNull(module, "module can't be null");

        return forName(module.getName());
    }

    /**
     * Sets the current logger level. Any message with a lower severity than this will not be printed.
     *
     * @param level The new level
     */
    public static void level(@NotNull LogLevel level) {
        Objects.requireNonNull(level, "level can't be null");

        LEVEL = level;
    }
}

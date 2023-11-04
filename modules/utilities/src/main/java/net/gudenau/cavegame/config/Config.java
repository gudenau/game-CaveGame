package net.gudenau.cavegame.config;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.Platform;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * A simple configuration system.
 *
 * @param <T> The type of this configuration value
 */
public final class Config<T> {
    private static final BooleanSupplier TRUE = () -> Boolean.TRUE;

    /**
     * All known configuration values.
     */
    private static final Set<Config<?>> CONFIGURATION = new HashSet<>();

    /**
     * Enable debug mode, this causes native memory allocations to be checked as well as more checks in various
     * libraries and code paths to be run. This may cause performance degradation.
     */
    public static final Config<Boolean> DEBUG = bool("debug", false, TRUE);

    /**
     * Sets the maximum native buffer size used for reading files. Setting this too small will prevent assets from being
     * loaded, setting it to large may allow for DoS.
     */
    public static final Config<Integer> MAX_BUFFER_SIZE = integer("max_buffer_size", 0x10000000, TRUE);

    /**
     * The global log level of this program.
     */
    public static final Config<String> LOG_LEVEL = string("log_level", "debug", TRUE); // TODO Enums?

    public static final Config<String> RENDERER = string("renderer", "CaveGameVk", TRUE);

    /**
     * A flag to force the use of X on Linux systems that have Wayland installed.
     */
    public static final Config<Boolean> FORCE_X = bool("force_x", false, () -> Platform.get() == Platform.LINUX);

    static {
        // Check system props for any matching values
        CONFIGURATION.stream()
            .filter(Config::supported)
            .forEachOrdered((config) -> {
                var prop = System.getProperty("cavegame." + config.name);
                if(prop != null) {
                    config.deserialize(prop);
                }
            });
    }

    /**
     * Parses the command line arguments and sets the configuration options appropriately.
     *
     * @param args The arguments to parse
     * @throws RuntimeException if there was an error parsing a value pair
     */
    public static void parseArguments(@NotNull String @NotNull [] args) {
        if(args.length == 0) {
            return;
        }

        Map<String, String> values = new HashMap<>();
        for(int i = 0, length = args.length; i < length; i++) {
            var arg = args[i];
            if(!arg.startsWith("-")) {
                throw new RuntimeException("Illegal argument " + arg);
            }
            arg = arg.substring(1);

            int split = arg.indexOf('=');
            if(split != -1) {
                values.put(arg.substring(0, split), arg.substring(split + 1));
            } else {
                if(i + 1 == length) {
                    throw new RuntimeException("Missing value for -" + arg);
                }
                values.put(arg, args[++i]);
            }
        }
        CONFIGURATION.stream()
            .filter(Config::supported)
            .forEach((config) ->
                Optional.ofNullable(values.get(config.name))
                    .ifPresent(config::deserialize)
            );
    }

    /**
     * Creates a new boolean configuration value.
     *
     * @param name The name of the configuration value
     * @param value The default value
     * @param supported True if this config option should be exposed
     * @return The created configuration value
     */
    @Contract("_, _, _ -> new")
    private static Config<Boolean> bool(String name, boolean value, BooleanSupplier supported) {
        return new Config<>(name, value, Boolean::valueOf, supported);
    }

    /**
     * Creates a new integer configuration value.
     *
     * @param name The name of the configuration value
     * @param value The default value
     * @param supported True if this config option should be exposed
     * @return The created configuration value
     */
    @Contract("_, _, _ -> new")
    private static Config<Integer> integer(String name, int value, BooleanSupplier supported) {
        return new Config<>(name, value, Integer::valueOf, supported);
    }

    /**
     * Creates a new string configuration value.
     *
     * @param name The name of the configuration value
     * @param value The default value
     * @param supported True if this config option should be exposed
     * @return The created configuration value
     */
    @Contract("_, _, _ -> new")
    private static Config<String> string(String name, String value, BooleanSupplier supported) {
        return new Config<>(name, value, Function.identity(), supported);
    }

    /**
     * The name of this configuration option.
     */
    @NotNull
    private final String name;

    /**
     * The deserializer to use when parsing config values.
     */
    @NotNull
    private final Function<@NotNull String, @NotNull T> deserializer;

    /**
     * The value of this configuration option.
     */
    @NotNull
    private T value;

    /**
     * Used to determine if this config option should be exposed.
     */
    private final BooleanSupplier supported;

    private Config(@NotNull String name, @NotNull T value, @NotNull Function<@NotNull String, @NotNull T> deserializer, @NotNull BooleanSupplier supported) {
        this.name = name;
        this.deserializer = deserializer;
        this.value = value;
        this.supported = supported;

        CONFIGURATION.add(this);
    }

    /**
     * Gets the current value of this configuration.
     *
     * @return The current value
     */
    @NotNull
    public T get() {
        return value;
    }

    /**
     * Parses a string to set the current value of this option.
     *
     * @param value The string to parse
     */
    private void deserialize(@NotNull String value) {
        this.value = deserializer.apply(value);
    }

    /**
     * Checks if this config option should be exposed.
     *
     * @return true if this config option should be exposed
     */
    private boolean supported() {
        return supported.getAsBoolean();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

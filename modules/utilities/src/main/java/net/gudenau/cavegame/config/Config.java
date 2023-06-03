package net.gudenau.cavegame.config;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

public final class Config<T> {
    private static final Set<Config<?>> CONFIGURATION = new HashSet<>();

    public static final Config<Boolean> DEBUG = bool("debug", false);
    public static final Config<Integer> MAX_BUFFER_SIZE = integer("max_buffer_size", 0x10000000);

    static {
        CONFIGURATION.forEach((config) -> {
            var prop = System.getProperty("cavegame." + config.name);
            if(prop != null) {
                config.deserialize(prop);
            }
        });
    }

    public static void parseArguments(String[] args) {
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
        CONFIGURATION.forEach((config) ->
            Optional.ofNullable(values.get(config.name))
                .ifPresent(config::deserialize)
        );
    }

    private static Config<Boolean> bool(String name, boolean value) {
        return new Config<>(name, value, Boolean::valueOf);
    }

    private static Config<Integer> integer(String name, int value) {
        return new Config<>(name, value, Integer::valueOf);
    }

    @NotNull
    private final String name;

    @NotNull
    private final Function<@NotNull String, @NotNull T> deserializer;
    @NotNull
    private T value;
    private Config(@NotNull String name, @NotNull T value, @NotNull Function<@NotNull String, @NotNull T> deserializer) {
        this.name = name;
        this.deserializer = deserializer;
        this.value = value;

        CONFIGURATION.add(this);
    }

    @NotNull
    public T get() {
        return value;
    }

    private void deserialize(@NotNull String value) {
        this.value = deserializer.apply(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

package net.gudenau.cavegame.util;

import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class MiscUtils {
    private MiscUtils() {
        throw new AssertionError();
    }

    @NotNull
    public static String longClassName(@NotNull Class<?> type) {
        var module = type.getModule();
        if(module.isNamed()) {
            return module.getName() + '/' + type.getName();
        } else {
            return "UNAMED/" + type.getName();
        }
    }

    @NotNull
    public static String stringifyException(@NotNull Throwable exception) {
        var writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}

package net.gudenau.cavegame.panama.impl;

import net.gudenau.cavegame.panama.NativeUtils;
import net.gudenau.cavegame.panama.Posix;
import net.gudenau.cavegame.util.Lazy;

import java.lang.foreign.Arena;
import java.lang.invoke.MethodHandle;
import java.util.function.Supplier;

import static net.gudenau.cavegame.panama.NativeUtils.ADDRESS;
import static net.gudenau.cavegame.panama.NativeUtils.INT;

public final class WindowsPosix implements Posix {
    public static final Supplier<Posix> INSTANCE = Lazy.supplier(WindowsPosix::new);

    private final MethodHandle SetEnvironmentVariableA;

    private WindowsPosix() {
        var binder = NativeUtils.systemBinder();
        SetEnvironmentVariableA = binder.bind("SetEnvironmentVariableA", INT, ADDRESS, ADDRESS);
    }

    @Override
    public void setenv(String name, String value, boolean overwrite) {
        int result;
        try(var arena = Arena.ofConfined()) {
            result = (int) SetEnvironmentVariableA.invokeExact(
                arena.allocateFrom(name),
                arena.allocateFrom(value)
            );
        } catch(Throwable e) {
            throw new RuntimeException("Failed to invoke SetEnvironmentVariableA", e);
        }
        if(result == 0) {
            throw new RuntimeException("Failed to set env var \"" + name + "\" to \"" + value + '"');
        }
    }
}

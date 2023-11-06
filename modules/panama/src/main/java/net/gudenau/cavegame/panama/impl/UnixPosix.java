package net.gudenau.cavegame.panama.impl;

import net.gudenau.cavegame.panama.NativeUtils;
import net.gudenau.cavegame.panama.Posix;
import net.gudenau.cavegame.util.Lazy;

import java.lang.foreign.Arena;
import java.lang.invoke.MethodHandle;
import java.util.function.Supplier;

import static net.gudenau.cavegame.panama.NativeUtils.ADDRESS;
import static net.gudenau.cavegame.panama.NativeUtils.INT;

public final class UnixPosix implements Posix {
    public static final Supplier<Posix> INSTANCE = Lazy.supplier(UnixPosix::new);

    private final MethodHandle setenv;

    private UnixPosix() {
        var binder = NativeUtils.systemBinder();
        setenv = binder.bind("setenv", INT, ADDRESS, ADDRESS, INT);
    }

    @Override
    public void setenv(String name, String value, boolean overwrite) {
        int result;
        try(var arena = Arena.ofConfined()) {
            result = (int) setenv.invokeExact(
                arena.allocateFrom(name),
                arena.allocateFrom(value),
                overwrite ? 1 : 0
            );
        } catch(Throwable e) {
            throw new RuntimeException("Failed to invoke setenv", e);
        }
        if(result != 0) {
            throw new RuntimeException("Failed to set env var \"" + name + "\" to \"" + value + '"');
        }
    }
}

package net.gudenau.cavegame.panama;

import net.gudenau.cavegame.panama.impl.UnixPosix;
import net.gudenau.cavegame.panama.impl.WindowsPosix;
import org.lwjgl.system.Platform;

public sealed interface Posix permits UnixPosix, WindowsPosix {
    static Posix instance() {
        return (Platform.get() == Platform.WINDOWS ? WindowsPosix.INSTANCE : UnixPosix.INSTANCE).get();
    }

    void setenv(String name, String value, boolean overwrite);
}

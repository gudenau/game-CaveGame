package net.gudenau.cavegame.panama;

import net.gudenau.cavegame.util.MiscUtils;
import net.gudenau.cavegame.util.Treachery;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.Platform;

import java.io.IOException;
import java.lang.foreign.AddressLayout;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.ValueLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class NativeUtils {
    public static final AddressLayout ADDRESS = ValueLayout.ADDRESS;
    public static final MemoryLayout INT;
    public static final MemoryLayout LONG;
    public static final MemoryLayout LONG_LONG;
    public static final MemoryLayout SIZE_T;

    static {
        var layouts = Linker.nativeLinker().canonicalLayouts();
        INT = layouts.get("int");
        LONG = layouts.get("long");
        LONG_LONG = layouts.get("long long");
        SIZE_T = layouts.get("size_t");
    }

    private record LibKey(@NotNull Module module, @NotNull String name) {
        private LibKey(@NotNull String name) {
            this(MiscUtils.getCaller(1).getModule(), name);
        }
    }

    private static final Path TEMP_DIR = MiscUtils.createTempDir("natives");
    private static final Map<LibKey, NativeBinder> BINDERS = new HashMap<>();

    private NativeUtils() {
        throw new AssertionError();
    }

    @NotNull
    public static NativeBinder load(@NotNull String library) {
        synchronized(BINDERS) {
            return BINDERS.computeIfAbsent(new LibKey(library), NativeUtils::doLoad);
        }
    }

    @NotNull
    private static NativeBinder doLoad(LibKey libKey) {
        var module = libKey.module();
        var library = libKey.name();
        var platform = Platform.get();
        var arch = Platform.getArchitecture();

        var libName = module.getName();
        libName = libName.substring(libName.lastIndexOf('.') + 1);

        var platformName = switch(platform) {
            case LINUX -> "linux";
            case MACOSX -> "osx";
            case WINDOWS -> "windows";
        };

        var archName = switch(arch) {
            case X64 -> "amd64";
            case X86 -> "x86";
            case ARM64 -> "aarch64";
            case ARM32 -> "arm";
        };

        library = System.mapLibraryName(library);
        var path = TEMP_DIR.resolve(Path.of(libName, library));
        try {
            Files.createDirectories(path.getParent());

           try(
               var output = Files.newOutputStream(path);
               var input = Treachery.getResourceAsStream(module, "/natives/" + libName + '/' + platformName + '/' + archName + '/' + library);
           ) {
                input.transferTo(output);
           }
        } catch(IOException e) {
            throw new RuntimeException("Failed to extract natives for " + module.getName());
        }

        return new NativeBinder(path);
    }
}

package net.gudenau.cavegame.panama;

import net.gudenau.cavegame.util.MiscUtils;
import net.gudenau.cavegame.util.Treachery;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.Platform;

import java.io.IOException;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.Buffer;
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
    private static final NativeBinder SYSTEM_BINDER = new NativeBinder(Linker.nativeLinker().defaultLookup(), "system");

    private NativeUtils() {
        throw new AssertionError();
    }

    @NotNull
    public static NativeBinder systemBinder() {
        enableNativeAccess(MiscUtils.getCaller(0).getModule());
        return SYSTEM_BINDER;
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
        enableNativeAccess(module);
        var library = libKey.name();
        var platform = Platform.get();
        var arch = Platform.getArchitecture();

        var libName = module.getName();
        libName = libName.substring(libName.lastIndexOf('.') + 1);

        var platformName = switch(platform) {
            case FREEBSD -> "freebsd";
            case LINUX -> "linux";
            case MACOSX -> "osx";
            case WINDOWS -> "windows";
        };

        var archName = switch(arch) {
            case X64 -> "amd64";
            case X86 -> "x86";
            case ARM64 -> "aarch64";
            case ARM32 -> "arm";
            case PPC64LE -> "ppc64le";
            case RISCV64 -> "riscv64";
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
            throw new RuntimeException("Failed to extract natives for " + module.getName(), e);
        }

        return new NativeBinder(path);
    }

    private static final MethodHandle Module$enableNativeAccess;
    static {
        Module$enableNativeAccess = Treachery.findVirtualSetterUnchecked(Module.class, "enableNativeAccess", boolean.class);
    }

    private static void enableNativeAccess(Module module) {
        try {
            Module$enableNativeAccess.invokeExact(module, true);
        } catch(Throwable e) {
            throw new RuntimeException("Failed to enable native access for module " + module.getName(), e);
        }
    }

    /**
     * Ensures that a MemorySegment is both non-null in Java land and C land.
     *
     * @param segment The segment to check
     * @param message The message to pass to the exception
     * @return The passed segment
     */
    @Contract("_, _ -> param1")
    @NotNull
    @SuppressWarnings("ConstantValue")
    public static MemorySegment ensureNonNull(@NotNull MemorySegment segment, @NotNull String message) {
        if(segment == null || segment.equals(MemorySegment.NULL)) {
            throw new NullPointerException(message);
        }
        return segment;
    }

    /**
     * Gets a String or null from the passed memory segment.
     *
     * @param pointer The segment to read
     * @return The string or null
     */
    @Nullable
    public static String string(@NotNull MemorySegment pointer) {
        //TODO Find a better way, this is hacky.
        if (pointer.equals(MemorySegment.NULL)) {
            return null;
        }
        return pointer.reinterpret(Long.MAX_VALUE).getString(0);
    }

    /**
     * Gets the {@link MemorySegment} from the provided {@link Buffer}. The buffer must be direct and be read-write.
     *
     * @param buffer The buffer to get
     * @return The resulting segment
     */
    @NotNull
    public static MemorySegment segment(@Nullable Buffer buffer) {
        if(buffer == null) {
            return MemorySegment.NULL;
        }
        if(!buffer.isDirect()) {
            throw new IllegalArgumentException(buffer.getClass().getSimpleName() + " must be direct");
        }
        if(buffer.isReadOnly()) {
            throw new IllegalArgumentException(buffer.getClass().getSimpleName() + " must be writeable");
        }
        return MemorySegment.ofBuffer(buffer);
    }

    @NotNull
    public static MemoryLayout struct(@NotNull MemoryLayout @NotNull ... elements) {
        return MemoryLayout.structLayout(elements);
    }
}

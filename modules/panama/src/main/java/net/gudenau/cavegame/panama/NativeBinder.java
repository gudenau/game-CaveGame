package net.gudenau.cavegame.panama;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;

public final class NativeBinder {
    private static final Linker LINKER = Linker.nativeLinker();

    private final Arena arena = Arena.ofAuto();
    private final SymbolLookup lookup;
    private final String library;

    NativeBinder(Path path) {
        lookup = SymbolLookup.libraryLookup(path, arena);
        library = path.getFileName().toString();
    }

    private MemorySegment symbol(String name) {
        return lookup.find(name)
            .orElseThrow(() -> new RuntimeException("Failed to find symbol " + name + " in " + library));
    }

    public MethodHandle bind(MemorySegment segment, FunctionDescriptor descriptor) {
        return LINKER.downcallHandle(segment, descriptor);
    }

    public MethodHandle bind(String name, FunctionDescriptor descriptor) {
        return LINKER.downcallHandle(symbol(name), descriptor);
    }

    @NotNull
    public MethodHandle bind(@NotNull String name, @Nullable MemoryLayout result, @NotNull MemoryLayout @NotNull ... args) {
        return bind(name, result == null ? FunctionDescriptor.ofVoid(args) : FunctionDescriptor.of(result, args));
    }

    public MemorySegment callback(MethodHandle methodHandle, FunctionDescriptor descriptor, @NotNull Arena arena) {
        return LINKER.upcallStub(methodHandle, descriptor, arena);
    }
}

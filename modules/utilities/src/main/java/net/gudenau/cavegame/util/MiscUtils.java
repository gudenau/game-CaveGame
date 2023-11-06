package net.gudenau.cavegame.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

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

    private static final Path TEMP_BASE = createTemp();

    @NotNull
    private static Path createTemp() {
        try {
            var temp = Files.createTempDirectory("cavegame");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Files.walkFileTree(temp, new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            try {
                                Files.deleteIfExists(file);
                            }catch(Throwable ignored) {}

                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                            return visitFile(dir, null);
                        }
                    });
                } catch(IOException ignored) {}
            }, "Temp Cleanup"));
            return temp;
        } catch(IOException e) {
            throw new RuntimeException("Failed to create temp directory", e);
        }
    }

    @NotNull
    public static Path createTempDir(@NotNull String name) {
        try {
            var path = TEMP_BASE.resolve(name);
            Files.createDirectories(path);
            return path;
        } catch(IOException e) {
            throw new RuntimeException("Failed to create temp directory: " + name, e);
        }
    }

    public static Class<?> getCaller(int skip) {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk((stream) ->
            stream.map(StackWalker.StackFrame::getDeclaringClass)
                .filter((klass) -> !klass.getModule().getName().matches("^java\\.|jdk\\.|javax\\."))
                .skip(2 + skip)
                .findFirst()
                .orElseThrow()
        );
    }
}

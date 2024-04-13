package net.gudenau.cavegame.renderer.gl.shader;

import net.gudenau.cavegame.resource.Identifier;
import net.gudenau.cavegame.resource.ResourceLoader;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL33C.*;

public final class GlShader implements AutoCloseable {
    private final int handle;

    public GlShader(@NotNull Type type, @NotNull Identifier identifier) {
        var resource = identifier.normalize("shader", '.' + type.extension);

        ByteBuffer code;
        try {
            code = ResourceLoader.buffer(resource);
        } catch(IOException e) {
            throw new RuntimeException("Failed to read shader " + resource, e);
        }
        try(var stack = MemoryStack.stackPush()) {
            handle = glCreateShader(type.id);
            glShaderSource(handle, stack.pointers(code), stack.ints(code.remaining()));
            glCompileShader(handle);

            if(glGetShaderi(handle, GL_COMPILE_STATUS) != GL_TRUE) {
                var error = glGetShaderInfoLog(handle).trim();
                throw new RuntimeException("Failed to compile " + type.name().toLowerCase(Locale.ROOT) + " shader " + resource + ": " + error);
            }
        } finally {
            MemoryUtil.memFree(code);
        }
    }

    public int handle() {
        return handle;
    }

    @Override
    public void close() {
        glDeleteShader(handle);
    }

    public enum Type {
        FRAGMENT(GL_FRAGMENT_SHADER, "frag", true),
        VERTEX(GL_VERTEX_SHADER, "vert", true),
        ;

        public static final Set<Type> REQUIRED = Stream.of(values())
            .filter((value) -> value.required)
            .collect(Collectors.toUnmodifiableSet());

        private final int id;
        @NotNull
        private final String extension;
        private final boolean required;

        Type(int id, @NotNull String extension, boolean required) {
            this.id = id;
            this.extension = extension;
            this.required = required;
        }
    }
}

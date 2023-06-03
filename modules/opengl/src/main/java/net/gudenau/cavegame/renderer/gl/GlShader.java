package net.gudenau.cavegame.renderer.gl;

import net.gudenau.cavegame.resource.Identifier;
import net.gudenau.cavegame.resource.ResourceLoader;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Locale;

import static org.lwjgl.opengl.GL33.*;

public final class GlShader implements AutoCloseable {
    private final int handle;

    public GlShader(@NotNull Type type, @NotNull Identifier identifier) throws IOException {
        //TODO Name this and shove it into Identifier.
        var filename = identifier.filename();
        var directory = identifier.directory();
        var namespace = identifier.namespace();
        Identifier resource;
        if(directory == null) {
            resource = new Identifier(namespace, "shader/" + filename + "/opengl." + type.extension);
        } else {
            resource = new Identifier(namespace, "shader/" + directory + "/" + filename + "/opengl." + type.extension);
        }

        var code = ResourceLoader.buffer(resource);
        try(var stack = MemoryStack.stackPush()) {
            handle = glCreateShader(type.id);
            glShaderSource(handle, stack.pointers(code), (IntBuffer) null);
            glCompileShader(handle);

            if(glGetShaderi(handle, GL_COMPILE_STATUS) != GL_TRUE) {
                var error = glGetShaderInfoLog(handle);
                throw new RuntimeException("Failed to compile " + type.name().toLowerCase(Locale.ROOT) + " shader " + identifier + ": " + error);
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
        FRAGMENT(GL_FRAGMENT_SHADER, "frag"),
        VERTEX(GL_VERTEX_SHADER, "vert"),
        ;

        private final int id;
        @NotNull
        private final String extension;

        Type(int id, @NotNull String extension) {
            this.id = id;
            this.extension = extension;
        }
    }
}

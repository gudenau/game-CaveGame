package net.gudenau.cavegame.renderer.gl;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.gudenau.cavegame.util.SharedLock;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.Checks;

import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.system.MemoryUtil.NULL;

//TODO Make this actually cache state.
public final class GlState {
    private static final SharedLock LOCK = new SharedLock();
    private static final Long2ObjectMap<GlState> STATE = new Long2ObjectOpenHashMap<>();

    @NotNull
    public static GlState get() {
        var context = GLFW.glfwGetCurrentContext();
        if(context == NULL) {
            throw new IllegalStateException("No current GL context");
        }
        return get(context);
    }

    @NotNull
    public static GlState get(long context) {
        var state = LOCK.read(() -> STATE.get(context));
        if(state != null) {
            return state;
        }

        return LOCK.write(() -> STATE.computeIfAbsent(context, (key) -> new GlState()));
    }

    public static void remove(long context) {
        LOCK.write(() -> STATE.remove(context));
    }

    private int boundProgram = 0;

    public void bindProgram(int program) {
        if(boundProgram != program) {
            boundProgram = program;
            glUseProgram(boundProgram);
        }
    }

    public int boundProgram() {
        if(Checks.CHECKS) {
            if(boundProgram != glGetInteger(GL_CURRENT_PROGRAM)) {
                throw new IllegalStateException("Cached program did not match bound program!");
            }
        }

        return boundProgram;
    }

    private int activeTexture = GL_TEXTURE0;
    public void activeTexture(int texture) {
        if(texture < GL_TEXTURE0 || texture > GL_TEXTURE31) {
            throw new IllegalArgumentException("Bad texture " + texture);
        }
        if(texture != activeTexture) {
            glActiveTexture(texture);
            activeTexture = texture;
        }
    }

    public int activeTexture() {
        return activeTexture;
    }

    private static final int BOUND_TEXTURE_COUNT = GL_TEXTURE31 - GL_TEXTURE0;
    private final int[] boundTextures = new int[BOUND_TEXTURE_COUNT];
    public void bindTexture(int texture) {
        var active = activeTexture - GL_TEXTURE0;
        if(boundTextures[active] != texture) {
            glBindTexture(GL_TEXTURE_2D, texture);
            boundTextures[active] = texture;
        }
    }

    public int boundTexture() {
        var active = activeTexture - GL_TEXTURE0;
        if(Checks.CHECKS) {
            if(boundTextures[active] != glGetInteger(GL_TEXTURE_BINDING_2D)) {
                throw new IllegalStateException("Cached texture did not match bound texture!");
            }
        }

        return boundTextures[active];
    }

    private int boundArrayBuffer = 0;
    private int boundElementBuffer = 0;

    public void bindBuffer(int type, int handle) {
        switch(type) {
            case GL_ARRAY_BUFFER -> {
                if(this.boundArrayBuffer != handle) {
                    glBindBuffer(GL_ARRAY_BUFFER, handle);
                    this.boundArrayBuffer = handle;
                }
            }
            case GL_ELEMENT_ARRAY_BUFFER -> {
                if(this.boundElementBuffer != handle) {
                    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, handle);
                    this.boundElementBuffer = handle;
                }
            }
            default -> throw new IllegalArgumentException("Unknown buffer type: " + Integer.toHexString(type));
        }
    }

    public int boundBuffer(int type) {
        return switch(type) {
            case GL_ARRAY_BUFFER -> {
                if(Checks.CHECKS) {
                    if(boundArrayBuffer != glGetInteger(GL_ARRAY_BUFFER_BINDING)) {
                        throw new IllegalStateException("Cached array buffer did not match bound buffer!");
                    }
                }

                yield boundArrayBuffer;
            }
            case GL_ELEMENT_ARRAY_BUFFER -> {
                if(Checks.CHECKS) {
                    if(boundArrayBuffer != glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING)) {
                        throw new IllegalStateException("Cached element buffer did not match bound buffer!");
                    }
                }

                yield boundArrayBuffer;
            }
            default -> throw new IllegalArgumentException("Unknown buffer type: " + Integer.toHexString(type));
        };
    }

    private int boundVao;

    public void bindVao(int vao) {
        if(boundVao != vao) {
            glBindVertexArray(vao);
            boundVao = vao;
        }
    }

    public int boundVao() {
        return boundVao;
    }
}

package net.gudenau.cavegame.renderer.gl;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.gudenau.cavegame.util.SharedLock;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.Checks;

import static org.lwjgl.opengl.GL33.*;
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

    private int boundTexture = 0;
    public void bindTexture(int texture) {
        if(boundTexture != texture) {
            glBindTexture(GL_TEXTURE_2D, texture);
            this.boundTexture = texture;
        }
    }

    public int boundTexture() {
        if(Checks.CHECKS) {
            if(boundTexture != glGetInteger(GL_TEXTURE_BINDING_2D)) {
                throw new IllegalStateException("Cached texture did not match bound texture!");
            }
        }

        return boundTexture;
    }
}

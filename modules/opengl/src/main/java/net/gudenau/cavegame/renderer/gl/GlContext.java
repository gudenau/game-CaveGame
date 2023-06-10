package net.gudenau.cavegame.renderer.gl;

import net.gudenau.cavegame.config.Config;
import net.gudenau.cavegame.logger.LogLevel;
import net.gudenau.cavegame.logger.Logger;
import net.gudenau.cavegame.renderer.GlfwWindow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL44.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public sealed class GlContext extends GlfwWindow permits GlExecutor.Context, GlRenderer.PrimordialContext, GlWindow {
    private static final Logger LOGGER = Logger.forName("OpenGL");

    // There is a LWJGL race condition involving this, if LWJGL says this isn't freed it is lying.
    private static final GLDebugMessageCallback DEBUG_CALLBACK = GLDebugMessageCallback.create((source, type, id, severity, length, message, userParam) -> {
        var level = switch (severity) {
            case GL_DEBUG_SEVERITY_HIGH -> LogLevel.ERROR;
            case GL_DEBUG_SEVERITY_MEDIUM -> LogLevel.WARN;
            case GL_DEBUG_SEVERITY_NOTIFICATION -> LogLevel.DEBUG;
            default -> LogLevel.INFO;
        };

        LOGGER.log(level, MemoryUtil.memUTF8(message, length), new Throwable());
    });
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(DEBUG_CALLBACK::close, "GlContext Debug Cleanup"));
    }

    @NotNull
    private final GLCapabilities capabilities;

    @Nullable
    private Thread currentThread;

    public GlContext(@NotNull String title, int width, int height) {
        this(null, title, width, height);
    }

    public GlContext(@Nullable GlContext parent, @NotNull String title, int width, int height) {
        super(title, width, height, parent);

        var oldContext = glfwGetCurrentContext();
        var oldCaps = oldContext == NULL ? null : GL.getCapabilities();

        glfwMakeContextCurrent(handle());
        try {
            capabilities = GL.createCapabilities();

            if(Config.DEBUG.get()) {
                glEnable(GL_DEBUG_OUTPUT);
                glDebugMessageCallback(DEBUG_CALLBACK, NULL);
            }
        } finally {
            glfwMakeContextCurrent(oldContext);
            GL.setCapabilities(oldCaps);
        }
    }

    @Override
    protected long createWindow(String title, int width, int height, Object user) {
        var parent = (GlContext) user;

        glfwDefaultWindowHints();

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 4);
        glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, Config.DEBUG.get() ? GLFW_TRUE : GLFW_FALSE);

        //TODO Remove GL string
        return glfwCreateWindow(width, height, title + " (GL)", NULL, parent == null ? NULL : parent.handle());
    }

    @Override
    public void bind() {
        synchronized (this) {
            if (currentThread != null && currentThread != Thread.currentThread()) {
                throw new IllegalStateException("GL context was already bound to thread " + currentThread.getName());
            }

            glfwMakeContextCurrent(handle());
            GL.setCapabilities(capabilities);
        }
    }

    @Override
    public void release() {
        synchronized (this) {
            if(glfwGetCurrentContext() != handle()) {
                return;
            }

            currentThread = null;
            glfwMakeContextCurrent(NULL);
            GL.setCapabilities(null);
        }
    }

    @Override
    public void flip() {}

    @Override
    public void close() {
        super.close();
        GlState.remove(handle());
    }
}

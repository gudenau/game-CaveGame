package net.gudenau.cavegame.renderer.gl;

import net.gudenau.cavegame.renderer.Renderer;
import net.gudenau.cavegame.renderer.Shader;
import net.gudenau.cavegame.renderer.Window;
import net.gudenau.cavegame.resource.Identifier;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.lwjgl.opengl.GL44.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class GlRenderer implements Renderer {
    @NotNull
    private final GlContext primordialContext;
    @NotNull
    private final GlExecutor executor;
    @NotNull
    private final GlProgram shader;
    private int vao = 0;

    public GlRenderer(Window window) {
        if(!(window instanceof GlContext context)) {
            throw new IllegalArgumentException("Window " + window + " was not a GlContext");
        }
        primordialContext = new PrimordialContext(context);
        executor = new GlExecutor(primordialContext);

        shader = executor.schedule(() -> {
            try(
                var vertex = new GlShader(GlShader.Type.VERTEX, new Identifier(Identifier.CAVEGAME_NAMESPACE, "basic"));
                var fragment = new GlShader(GlShader.Type.FRAGMENT, new Identifier(Identifier.CAVEGAME_NAMESPACE, "basic"))
            ) {
                return new GlProgram(vertex, fragment);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).join();
    }

    @Override
    public void draw() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glClearColor(0, 0, 0, 1);

        //TODO These are per-context...
        if(vao == 0) {
            vao = glGenVertexArrays();
        }

        glBindVertexArray(vao);

        shader.bind();
        glDrawArrays(GL_TRIANGLES, 0, 3);
        shader.release();

        glBindVertexArray(0);
    }

    @Override
    public Optional<Shader> loadShader(@NotNull Identifier basic) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        glDeleteVertexArrays(vao);
        executor.close();
        primordialContext.close();
    }

    public static final class PrimordialContext extends GlContext {
        public PrimordialContext(GlContext window) {
            super(window, "CaveGame Primordial Context", 1, 1);
        }

        @Override
        public void bind() {
            throw new UnsupportedOperationException("Can not bind the primordial context");
        }
    }
}

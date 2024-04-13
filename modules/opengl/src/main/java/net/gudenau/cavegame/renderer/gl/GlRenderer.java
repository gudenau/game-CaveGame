package net.gudenau.cavegame.renderer.gl;

import net.gudenau.cavegame.renderer.*;
import net.gudenau.cavegame.renderer.gl.shader.GlProgram;
import net.gudenau.cavegame.renderer.gl.shader.GlShader;
import net.gudenau.cavegame.renderer.gl.shader.GlVertexFormat;
import net.gudenau.cavegame.renderer.shader.Shader;
import net.gudenau.cavegame.renderer.shader.ShaderMeta;
import net.gudenau.cavegame.renderer.texture.Texture;
import net.gudenau.cavegame.renderer.texture.TextureManager;
import net.gudenau.cavegame.resource.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL44C.*;

public final class GlRenderer implements Renderer {
    @NotNull
    private final GlTextureManager textureManager;
    @NotNull
    private final GlContext primordialContext;
    @NotNull
    private final GlExecutor executor;
    @NotNull
    private final GlWindow window;

    public GlRenderer(Window window) {
        if(!(window instanceof GlContext context)) {
            throw new IllegalArgumentException("Window " + window + " was not a GlContext");
        }
        this.window = (GlWindow) window;
        primordialContext = new PrimordialContext(context);
        executor = new GlExecutor(primordialContext);
        textureManager = new GlTextureManager(executor);
    }

    @Override
    public void draw() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glClearColor(0, 0, 0, 1);
    }

    //TODO Move this code somewhere else
    @Override
    public Shader loadShader(@NotNull Identifier identifier, @NotNull Map<String, Texture> textures) {
        ShaderMeta metadata;
        {
            var result = ShaderMeta.load(identifier);
            var partial = result.partial();
            if (partial.isPresent()) {
                throw new RuntimeException("Failed to load shader metadata for " + identifier + '\n' + partial.get().error());
            }
            metadata = result.getResult();
        }

        return executor.get((state) -> {
            var shaders = new HashMap<GlShader.Type, GlShader>();
            GlProgram program;
            try {
                var required = new HashSet<>(GlShader.Type.REQUIRED);
                metadata.shaders().forEach((type, info) -> {
                    var glType = switch(type) {
                        case VERTEX -> GlShader.Type.VERTEX;
                        case FRAGMENT -> GlShader.Type.FRAGMENT;
                    };
                    required.remove(glType);
                    shaders.put(glType, new GlShader(
                        glType,
                        info.files().get("opengl")
                    ));
                });
                if(!required.isEmpty()) {
                    throw new RuntimeException(
                        "Failed to load shader, required shaders where missing: " +
                            required.stream()
                                .map((missing) -> missing.name().toLowerCase())
                                .collect(Collectors.joining(", "))
                    );
                }

                program = new GlProgram(this, identifier, textures, shaders.values(), metadata);
            } finally {
                shaders.values().forEach(GlShader::close);
            }

            return program;
        });
    }

    @NotNull
    @Override
    public GlGraphicsBuffer createBuffer(@NotNull BufferType type, int size) {
        return new GlGraphicsBuffer(executor, type, size);
    }

    @Override
    public void drawBuffer(int vertexCount, @NotNull GraphicsBuffer vertexBuffer, @Nullable GraphicsBuffer indexBuffer) {
        var shader = (GlProgram) vertexBuffer.shader();
        if(indexBuffer != null && indexBuffer.shader() != shader) {
            throw new IllegalArgumentException("vertexBuffer and indexBuffer where for different shaders");
        }

        executor.run((state) -> {
            state.bindBuffer(GL_ARRAY_BUFFER, ((GlGraphicsBuffer) vertexBuffer).handle());
            if(indexBuffer != null) {
                state.bindBuffer(GL_ELEMENT_ARRAY_BUFFER, ((GlGraphicsBuffer) indexBuffer).handle());
            }

            shader.bind();

            glDrawArrays(GL_TRIANGLES, 0, vertexCount);

            state.bindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
            state.bindBuffer(GL_ARRAY_BUFFER, 0);
        });
    }

    @Override
    public void begin() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public void waitForIdle() {
        glFinish();
    }

    @Override
    public @NotNull TextureManager textureManager() {
        return textureManager;
    }

    @Override
    @NotNull
    public Window.Size framebufferSize() {
        return window.framebufferSize();
    }

    @Override
    public void close() {
        textureManager.close();
        executor.close();
        primordialContext.close();
    }

    @NotNull
    public GlExecutor executor() {
        return executor;
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

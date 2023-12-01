package net.gudenau.cavegame.renderer.gl;

import net.gudenau.cavegame.renderer.*;
import net.gudenau.cavegame.renderer.gl.shader.GlProgram;
import net.gudenau.cavegame.renderer.gl.shader.GlShader;
import net.gudenau.cavegame.renderer.shader.Shader;
import net.gudenau.cavegame.renderer.shader.ShaderMeta;
import net.gudenau.cavegame.renderer.texture.Texture;
import net.gudenau.cavegame.renderer.texture.TextureManager;
import net.gudenau.cavegame.resource.Identifier;
import net.gudenau.cavegame.util.collection.FastCollectors;
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
    private int vao = 0;

    public GlRenderer(Window window) {
        if(!(window instanceof GlContext context)) {
            throw new IllegalArgumentException("Window " + window + " was not a GlContext");
        }
        primordialContext = new PrimordialContext(context);
        executor = new GlExecutor(primordialContext);
        textureManager = new GlTextureManager(executor);
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

        //shader.bind();
        glDrawArrays(GL_TRIANGLES, 0, 3);
        //shader.release();

        glBindVertexArray(0);
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

        return executor.run((state) -> {
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
    public GraphicsBuffer createBuffer(@NotNull BufferType type, int size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void drawBuffer(int vertexCount, @NotNull GraphicsBuffer vertexBuffer, @Nullable GraphicsBuffer indexBuffer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void begin() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void waitForIdle() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull TextureManager textureManager() {
        return textureManager;
    }

    @Override
    public void close() {
        if(vao != 0) {
            glDeleteVertexArrays(vao);
        }
        textureManager.close();
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

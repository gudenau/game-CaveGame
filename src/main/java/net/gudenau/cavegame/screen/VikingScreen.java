package net.gudenau.cavegame.screen;

import net.gudenau.cavegame.renderer.BufferBuilder;
import net.gudenau.cavegame.renderer.BufferType;
import net.gudenau.cavegame.renderer.GraphicsBuffer;
import net.gudenau.cavegame.renderer.Renderer;
import net.gudenau.cavegame.renderer.model.ObjLoader;
import net.gudenau.cavegame.renderer.screen.Screen;
import net.gudenau.cavegame.renderer.shader.Shader;
import net.gudenau.cavegame.renderer.texture.Texture;
import net.gudenau.cavegame.resource.Identifier;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

import static net.gudenau.cavegame.CaveGame.NAMESPACE;

public class VikingScreen implements Screen {
    @NotNull
    private final Texture texture;
    @NotNull
    private final Shader shader;
    private final int vertexCount;
    private final GraphicsBuffer vertexBuffer;
    private final GraphicsBuffer indexBuffer;

    public VikingScreen(Renderer renderer) {
        try {
            texture = renderer.textureManager().loadTexture(new Identifier(NAMESPACE, "viking_room"));
        } catch(IOException e) {
            throw new RuntimeException(e);
        }

        shader = renderer.loadShader(
            new Identifier(Identifier.CAVEGAME_NAMESPACE, "basic"),
            Map.of("texSampler", texture)
        );

        BufferBuilder builder;
        try {
            builder = ObjLoader.load(shader.builder(), new Identifier(NAMESPACE, "viking_room"));
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        vertexCount = builder.vertexCount();
        var triangleBuffers = builder.build();
        vertexBuffer = triangleBuffers.get(BufferType.VERTEX);
        indexBuffer = triangleBuffers.get(BufferType.INDEX);
    }

    @Override
    public void draw(@NotNull Renderer renderer) {
        renderer.drawBuffer(vertexCount, vertexBuffer, indexBuffer);
    }

    @Override
    public void close() {
        vertexBuffer.close();
        indexBuffer.close();
        shader.close();
        texture.close();
    }
}

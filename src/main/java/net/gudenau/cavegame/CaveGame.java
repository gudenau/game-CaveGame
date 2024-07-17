package net.gudenau.cavegame;

import net.gudenau.cavegame.config.Config;
import net.gudenau.cavegame.input.Wooting;
import net.gudenau.cavegame.logger.LogLevel;
import net.gudenau.cavegame.logger.Logger;
import net.gudenau.cavegame.renderer.BufferBuilder;
import net.gudenau.cavegame.renderer.BufferType;
import net.gudenau.cavegame.renderer.GlfwUtils;
import net.gudenau.cavegame.renderer.RendererInfo;
import net.gudenau.cavegame.renderer.model.ObjLoader;
import net.gudenau.cavegame.renderer.texture.PngReader;
import net.gudenau.cavegame.renderer.texture.Texture;
import net.gudenau.cavegame.renderer.texture.TextureFormat;
import net.gudenau.cavegame.resource.ClassPathResourceProvider;
import net.gudenau.cavegame.resource.Identifier;
import net.gudenau.cavegame.resource.ResourceLoader;
import net.gudenau.cavegame.util.Closer;
import net.gudenau.cavegame.util.MiscUtils;
import net.gudenau.cavegame.util.Treachery;
import org.lwjgl.system.Configuration;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class CaveGame {
    public static final String NAMESPACE = Identifier.CAVEGAME_NAMESPACE;

    private static final Logger LOGGER = Logger.forName("CaveGame");

    // I'm ignoring the warnings in this method because it's going to be remade at some point.
    public static void main(String[] args) {
        Config.parseArguments(args);

        Configuration.SHARED_LIBRARY_EXTRACT_PATH.set(MiscUtils.createTempDir("lwjgl").toString());
        if(Config.DEBUG.get()) {
            Configuration.DEBUG.set(true);
            Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
            Configuration.DEBUG_STACK.set(true);
        }
        Logger.level(Config.LOG_LEVEL.get());

        GlfwUtils.handoverMain(CaveGame::newMain);
    }

    private static void newMain() {
        ResourceLoader.registerProvider(NAMESPACE, ClassPathResourceProvider.of(CaveGame.class));

        LOGGER.info("Available renderers: ");
        var renderers = RendererInfo.availableRenderers();
        renderers.forEach((info) ->
            LOGGER.info(
                """
                    %s:
                        Supported: %s
                """.formatted(
                    info.name(),
                    info.supported() ? "true" : "false"
                )
            )
        );
        var rendererInfo = renderers.stream()
            .filter(RendererInfo::supported)
            .filter((info) -> info.name().equals(Config.RENDERER.get()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Failed to find supported renderer " + Config.RENDERER.get()));
        LOGGER.info("Using " + rendererInfo.name());

        try(var closer = new Closer()) {
            var window = closer.add(rendererInfo.createWindow("CaveGame", 640, 480));
            var renderer = closer.add(rendererInfo.createRenderer(window));
            var textureManager = renderer.textureManager();

            window.bind();

            Texture texture;
            try {
                texture = textureManager.loadTexture(new Identifier(NAMESPACE, "viking_room"));
            } catch(IOException e) {
                throw new RuntimeException(e);
            }

            var basicShader = closer.add(renderer.loadShader(
                new Identifier(Identifier.CAVEGAME_NAMESPACE, "basic"),
                Map.of("texSampler", texture)
            ));

            BufferBuilder builder;
            try {
                builder = ObjLoader.load(basicShader.builder(), new Identifier(NAMESPACE, "viking_room"));
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
            var vertexCount = builder.vertexCount();
            var triangleBuffers = builder.build();
            var vertexBuffer = triangleBuffers.get(BufferType.VERTEX);
            var indexBuffer = triangleBuffers.get(BufferType.INDEX);
            closer.add(vertexBuffer, indexBuffer);

            window.visible(true);

            do {
                renderer.begin();
                renderer.drawBuffer(vertexCount, vertexBuffer, indexBuffer);
                renderer.draw();

                window.flip();
                GlfwUtils.poll();
            } while(!window.closeRequested());

            renderer.waitForIdle();
        }
    }
}

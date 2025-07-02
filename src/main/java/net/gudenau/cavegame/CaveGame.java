package net.gudenau.cavegame;

import net.gudenau.cavegame.config.Config;
import net.gudenau.cavegame.logger.Logger;
import net.gudenau.cavegame.renderer.BufferBuilder;
import net.gudenau.cavegame.renderer.BufferType;
import net.gudenau.cavegame.renderer.GlfwUtils;
import net.gudenau.cavegame.renderer.RendererInfo;
import net.gudenau.cavegame.renderer.font.HarfBuzzFont;
import net.gudenau.cavegame.renderer.model.ObjLoader;
import net.gudenau.cavegame.renderer.texture.Font;
import net.gudenau.cavegame.renderer.texture.Texture;
import net.gudenau.cavegame.resource.ClassPathResourceProvider;
import net.gudenau.cavegame.resource.Identifier;
import net.gudenau.cavegame.resource.ResourceLoader;
import net.gudenau.cavegame.util.Closer;
import net.gudenau.cavegame.util.MiscUtils;
import org.lwjgl.system.Configuration;

import java.io.IOException;
import java.util.Map;

public final class CaveGame {
    public static final String NAMESPACE = Identifier.CAVEGAME_NAMESPACE;

    private static final Logger LOGGER = Logger.forName("CaveGame");

    public static void main(String[] args) {
        if(!CaveGame.class.getModule().isNamed()) {
            throw new IllegalStateException("Can not run as an unnamed module");
        }

        Config.parseArguments(args);

        Configuration.SHARED_LIBRARY_EXTRACT_PATH.set(MiscUtils.createTempDir("lwjgl").toString());
        if(Config.DEBUG.get()) {
            Configuration.DEBUG.set(true);
            Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
            Configuration.DEBUG_STACK.set(true);
        }

        // Required for HarfBuzz to support FreeType with LWJGL
        HarfBuzzFont.staticInit();

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

            Font font;
            try {
                font = textureManager.loadFont(new Identifier(NAMESPACE, "fira_sans_regular"));
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

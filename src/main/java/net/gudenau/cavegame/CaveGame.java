package net.gudenau.cavegame;

import net.gudenau.cavegame.config.Config;
import net.gudenau.cavegame.logger.Logger;
import net.gudenau.cavegame.renderer.*;
import net.gudenau.cavegame.renderer.model.ObjLoader;
import net.gudenau.cavegame.renderer.shader.Shader;
import net.gudenau.cavegame.renderer.texture.Texture;
import net.gudenau.cavegame.resource.ClassPathResourceProvider;
import net.gudenau.cavegame.resource.Identifier;
import net.gudenau.cavegame.resource.ResourceLoader;
import net.gudenau.cavegame.util.Closer;
import net.gudenau.cavegame.util.MiscUtils;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.lwjgl.system.Configuration;

import java.io.IOException;
import java.util.Map;

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

            var ubo = basicShader.uniformMvp()
                .orElseThrow(() -> new RuntimeException("Failed to find MVP uniform"));

            window.visible(true);

            do {
                updateUbo(renderer, ubo);

                renderer.begin();
                renderer.drawBuffer(vertexCount, vertexBuffer, indexBuffer);
                renderer.draw();
 
                window.flip();
                GlfwUtils.poll();
            } while(!window.closeRequested());

            renderer.waitForIdle();
        }













        /*
        Treachery.ensureInitialized(Registries.class, Tiles.class, Materials.class, JobTypes.class);

        Wooting.initialize();

        var futures = Registries.TILE.entries()
            .map((entry) -> ThreadPool.future(() -> {
                var key = entry.getKey();
                try(var stream = ResourceLoader.stream(key.prefixPath("texture/tile").append(".png"))) {
                    return Map.entry(key, ImageIO.read(stream));
                } catch (IOException e) {
                    throw new RuntimeException("Failed to open texture for tile " + key, e);
                }
            }))
            .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).join();

        @SuppressWarnings("unchecked")
        Map<Tile, BufferedImage> map = Stream.of(futures)
            .map((future) -> (CompletableFuture<Map.Entry<Identifier, BufferedImage>>) future)
            .map(CompletableFuture::join)
            .collect(Collectors.toUnmodifiableMap((entry) -> Registries.TILE.object(entry.getKey()).get(), Map.Entry::getValue));

        var level = new Level(16, 16);
        TilePos.iterator(1, 1, level.width() - 1, level.height() - 1)
            .forEachRemaining((pos) -> level.tile(pos, Tiles.DIRT_WALL));

        level.tile(new TilePos(5, 5), Tiles.STORE_ROOM);
        level.tile(new TilePos(5, 4), Tiles.FLOOR);
        level.tile(new TilePos(5, 6), Tiles.FLOOR);
        level.tile(new TilePos(4, 5), Tiles.FLOOR);
        level.tile(new TilePos(6, 5), Tiles.FLOOR);

        level.spawn(new MinerActor(5.5, 4.5, level));

        /*
        for(int i = 0; i < 2; i++) {
            level.spawn(new MinerActor(5.5, 4.5, level));
            level.spawn(new MinerActor(5.5, 6.5, level));
            level.spawn(new MinerActor(4.5, 5.5, level));
            level.spawn(new MinerActor(6.5, 5.5, level));
        }
         * /

        TilePos.iterator(0, 0, level.width(), level.height()).forEachRemaining((pos) -> {
            var tile = level.tile(pos);
            if(tile instanceof WallTile wall && wall.isMineable()) {
                level.jobManager().enqueueJob(new MiningJob(tile, pos));
            }
        });

        var state = new Object() {
            BufferedImage buffer = null;
        };

        SwingUtilities.invokeLater(() -> {
            var panel = new JPanel() {
                BufferedImage buffer;

                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    synchronized (state) {
                        if(state.buffer != null) {
                            buffer = state.buffer;
                            state.buffer = null;
                        }
                    }
                    if(buffer != null) {
                        g.drawImage(buffer, 0, 0, null);
                    }
                }
            };
            panel.setPreferredSize(new Dimension(level.width() * 32, level.height() * 32));

            var frame = new JFrame();
            frame.add(panel);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setResizable(false);
            frame.setVisible(true);

            var delay = 100;
            var timer = new Timer(delay, (event) -> panel.repaint());
            timer.setDelay(delay);
            timer.start();
        });

        while(!level.actors().isEmpty()) {
            var buffer = new BufferedImage(level.width() * 32, level.height() * 32, BufferedImage.TYPE_4BYTE_ABGR);
            var g = buffer.createGraphics();
            try {
                int width = level.width();
                int height = level.height();

                TilePos.iterator(0, 0, width, height).forEachRemaining((pos) -> {
                    var tile = level.tile(pos);
                    g.drawImage(map.get(tile), pos.x() << 5, pos.y() << 5, 32, 32, null);
                });

                level.actors().forEach((actor) -> {
                    /*
                    if (actor instanceof LivingActor) {
                        @SuppressWarnings("unchecked")
                        var nodes = (LinkedList<TilePos>) (Queue<TilePos>) Treachery.field(actor, "nodes", Queue.class);
                        for (int i = 0, limit = nodes.size(); i < limit; i++) {
                            TilePos node = nodes.get(i);
                            g.setColor(new Color(0, (int) ((1 - i / (double) limit) * 255), 0));
                            g.fillRect((node.x() << 5) + 8, (node.y() << 5) + 8, 16, 16);
                        }
                    }
                     * /

                    var x = actor.x() * 32;
                    var y = actor.y() * 32;
                    if(actor instanceof ResourceActor) {
                        g.setColor(Color.RED);
                    } else {
                        g.setColor(Color.ORANGE);
                    }
                    g.fillRect((int) (x - 4), (int) (y - 4), 8, 8);
                });
            } finally {
                g.dispose();
                synchronized (state) {
                    state.buffer = buffer;
                }
            }

            level.tick();

            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {}
        }
         */
    }

    private static long startTime = System.nanoTime();
    private static void updateUbo(@NotNull Renderer renderer, Shader.MVP uniform) {
        var currentTime = System.nanoTime();
        var delta = (float)((currentTime - startTime) * 1.0E-10);

        var ubo = new UniformBufferObject();
        ubo.model().rotate(
            delta * 1.5707963267948966F, // 90 degrees
            new Vector3f(0, 0, 1)
        );
        ubo.view().lookAt(
            new Vector3f(2, 2, 2),
            new Vector3f(0, 0, 0),
            new Vector3f(0, 0, 1)
        );
        var projection = ubo.proj();
        var framebufferSize = renderer.framebufferSize();
        projection.perspective(
            0.7853982F, // 45 degrees
            (float) framebufferSize.width() / framebufferSize.height(),
            0.1F,
            10,
            true
        );

        uniform.upload(ubo::write);
    }
}

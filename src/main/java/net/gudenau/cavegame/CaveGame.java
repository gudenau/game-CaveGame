package net.gudenau.cavegame;

import net.gudenau.cavegame.actor.MinerActor;
import net.gudenau.cavegame.actor.ResourceActor;
import net.gudenau.cavegame.ai.JobTypes;
import net.gudenau.cavegame.ai.MiningJob;
import net.gudenau.cavegame.config.Config;
import net.gudenau.cavegame.input.Wooting;
import net.gudenau.cavegame.level.Level;
import net.gudenau.cavegame.level.TilePos;
import net.gudenau.cavegame.logger.LogLevel;
import net.gudenau.cavegame.logger.Logger;
import net.gudenau.cavegame.material.Materials;
import net.gudenau.cavegame.renderer.GlfwUtils;
import net.gudenau.cavegame.renderer.RendererInfo;
import net.gudenau.cavegame.resource.ClassPathResourceProvider;
import net.gudenau.cavegame.resource.Identifier;
import net.gudenau.cavegame.resource.ResourceLoader;
import net.gudenau.cavegame.tile.Tile;
import net.gudenau.cavegame.tile.Tiles;
import net.gudenau.cavegame.tile.WallTile;
import net.gudenau.cavegame.util.*;
import org.lwjgl.system.Configuration;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CaveGame {
    public static final String NAMESPACE = Identifier.CAVEGAME_NAMESPACE;

    private static final Logger LOGGER = Logger.forName("CaveGame");

    // I'm ignoring the warnings in this method because it's going to be remade at some point.
    public static void main(String[] args) {
        Config.parseArguments(args);

        if(Config.DEBUG.get()) {
            Configuration.DEBUG.set(true);
            Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
            Configuration.DEBUG_STACK.set(true);
        }
        Logger.level(LogLevel.of(Config.LOG_LEVEL.get()));

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

        try(
            var window = rendererInfo.createWindow("CaveGame", 640, 480);
            var renderer = rendererInfo.createRenderer(window);
        ) {
            window.bind();
            window.visible(true);

            do {
                renderer.draw();

                window.flip();
                GlfwUtils.poll();
            } while(!window.closeRequested());
        }













        if(true || false) {
            return;
        }



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
         */

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
                     */

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
    }
}

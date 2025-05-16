package net.gudenau.cavegame.gui;

import net.gudenau.cavegame.gui.component.*;
import net.gudenau.cavegame.gui.drawing.*;
import net.gudenau.cavegame.gui.input.MouseButton;
import net.gudenau.cavegame.gui.layout.GridLayoutEngine;
import net.gudenau.cavegame.gui.value.Value;
import net.gudenau.cavegame.resource.ClassPathResourceProvider;
import net.gudenau.cavegame.resource.Identifier;
import net.gudenau.cavegame.resource.ResourceLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class GuiTest {
    private GuiTest() {
        throw new AssertionError();
    }

    public static void main(String[] args) throws Throwable {
        SwingUtilities.invokeAndWait(() -> {
            var frame = new JFrame("GUI Test");

            ResourceLoader.registerProvider("gui", ClassPathResourceProvider.of(GuiTest.class));

            var graphics = new AwtGraphics(640, 480);
            graphics.drawRectangle(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE, 0xFFFF00FF);

            var layout = GridLayoutEngine.builder(1, GridLayoutEngine.UNLIMITED)
                .minimumSize(graphics.width() / 2, graphics.height() / 2)
                .build();
            var container = new Container(layout) {
                @Override
                public void invalidate() {
                    super.invalidate();

                    frame.repaint();
                }
            };
            container.add(new TextComponent("Hello world!", graphics, FontStyle.of()));
            container.add(new TextComponent("Narrow", graphics, FontStyle.of()));
            container.add(new TextComponent("Centered", graphics, FontStyle.of(FontAttribute.horizontalAlignment(FontAttribute.Alignment.CENTER))));

            var buttonValue = Value.enumeration(Direction.RIGHT);
            var button = container.add(new ButtonComponent<>(new ValueComponent<>(buttonValue, graphics, FontStyle.of(
                FontAttribute.horizontalAlignment(FontAttribute.Alignment.CENTER),
                FontAttribute.verticalAlignment(FontAttribute.Alignment.CENTER)
            ))));
            button.action(buttonValue::next);

            var sliderValue = Value.enumeration(Direction.RIGHT);
            container.add(new SliderComponent<>(new ValueComponent<>(sliderValue, graphics, FontStyle.of(FontAttribute.horizontalAlignment(FontAttribute.Alignment.CENTER)))));

            frame.setSize(640, 480);
            frame.add(new JPanel() {
                {
                    var listener = new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            var location = e.getPoint();
                            var button = switch(e.getButton()) {
                                case MouseEvent.BUTTON1 -> MouseButton.LEFT;
                                case MouseEvent.BUTTON2 -> MouseButton.RIGHT;
                                case MouseEvent.BUTTON3 -> MouseButton.MIDDLE;
                                case 4 -> MouseButton.FOUR;
                                case 5 -> MouseButton.FIVE;
                                default -> null;
                            };
                            if(button != null) {
                                container.onClick(location.x, location.y, button);
                            }
                        }

                        @Override
                        public void mouseWheelMoved(MouseWheelEvent e) {
                            var location = e.getPoint();
                            container.onScroll(location.x, location.y, -e.getWheelRotation());
                        }
                    };
                    addMouseListener(listener);
                    addMouseMotionListener(listener);
                    addMouseWheelListener(listener);
                }

                @Override
                public void paint(java.awt.Graphics g) {
                    container.draw(graphics);
                    g.drawImage(graphics.root.canvas, 0, 0, null);
                }
            });
            frame.setResizable(false);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }

    private static final class AwtGraphics implements DrawContext, Font {
        private AwtCanvas root;
        private AwtCanvas canvas;

        private AwtGraphics(int width, int height) {
            resize(width, height);
        }

        public void resize(int width, int height) {
            root = canvas = new AwtCanvas(0, 0, null, width, height);
        }

        @Override
        public int width() {
            return canvas.canvas.getWidth();
        }

        @Override
        public int height() {
            return canvas.canvas.getHeight();
        }

        @Override
        public @NotNull StackEntry scissor(int x, int y, int width, int height) {
            canvas = new AwtCanvas(x, y, canvas, width, height);
            return canvas;
        }

        @Override
        public void drawRectangle(int x, int y, int width, int height, int color) {
            var graphics = canvas.graphics;
            graphics.setColor(new Color(color, true));
            graphics.fillRect(x, y, width, height);
        }

        @Override
        public void drawText(int x, int y, int width, int height, @NotNull String text, @NotNull FontStyle style) {
            var graphics = canvas.graphics;
            graphics.setColor(new Color(style.get(FontAttribute.Color.class).color(), true));

            if(width != -1 || height != -1) {
                var metrics = metrics(text);

                if(width != -1) {
                    switch(style.get(FontAttribute.HorizontalAlignment.class).alignment()) {
                        case NEG -> {}
                        case CENTER -> x += (width - metrics.width()) / 2;
                        case POS -> x += width - metrics.width();
                    }
                }

                if(height != -1) {
                    switch(style.get(FontAttribute.VerticalAlignment.class).alignment()) {
                        case NEG -> {}
                        case CENTER -> y += (height - metrics.height()) / 2;
                        case POS -> y += height - metrics.height();
                    }
                }
            }

            graphics.drawString(text, x, y);
        }

        private final Map<Identifier, BufferedImage> images = new HashMap<>();

        @NotNull
        private BufferedImage loadImage(@NotNull Identifier identifier) {
            var image = images.get(identifier);
            if(image != null) {
                return image;
            }

            try(var stream = ResourceLoader.stream(identifier.normalize("texture", ".png"))) {
                image = ImageIO.read(stream);
            } catch(IOException e) {
                throw new RuntimeException("Failed to load GUI image " + identifier, e);
            }

            images.put(identifier, image);

            return image;
        }

        @Override
        public void drawImage(@NotNull Identifier identifier, int x, int y, int width, int height, int u, int v, int uWidth, int uHeight, int textureWidth, int textureHeight) {
            var image = loadImage(identifier);

            int imageXOff = (int) ((u / (double) textureWidth) * image.getWidth());
            int imageYOff = (int) ((v / (double) textureHeight) * image.getHeight());
            int imageWidth = (int) (((uWidth) / (double) textureWidth) * image.getWidth());
            int imageHeight = (int) (((uHeight) / (double) textureHeight) * image.getHeight());

            var graphics = canvas.graphics;
            graphics.drawImage(image, x, y, x + width, y + height, imageXOff, imageYOff, imageXOff + imageWidth, imageYOff + imageHeight, null);
        }

        @Override
        @NotNull
        public TextMetrics lineMetrics(@NotNull String text) {
            var bounds = canvas.graphics.getFontMetrics().getStringBounds(text, canvas.graphics);
            return new TextMetrics(ascent(), (int) bounds.getWidth(), (int) bounds.getHeight());
        }

        @Override
        public int ascent() {
            return canvas.graphics.getFontMetrics().getAscent();
        }

        private final class AwtCanvas implements DrawContext.StackEntry {
            private final int x;
            private final int y;
            @Nullable
            private final AwtCanvas parent;

            private final BufferedImage canvas;
            private final Graphics2D graphics;

            private AwtCanvas(int x, int y, @Nullable AwtCanvas parent, int width, int height) {
                this.x = x;
                this.y = y;
                this.parent = parent;

                canvas = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
                graphics = canvas.createGraphics();
            }

            @Override
            public void close() {
                graphics.dispose();
                if(parent == null) {
                    return;
                }

                parent.graphics.drawImage(canvas, x, y, null);

                if(AwtGraphics.this.canvas != this) {
                    throw new IllegalStateException();
                }
                AwtGraphics.this.canvas = parent;
            }
        }
    }
}

package net.gudenau.cavegame.renderer.vk.gui;

import net.gudenau.cavegame.gui.drawing.DrawContext;
import net.gudenau.cavegame.gui.drawing.FontStyle;
import net.gudenau.cavegame.renderer.BufferBuilder;
import net.gudenau.cavegame.renderer.GraphicsBuffer;
import net.gudenau.cavegame.renderer.Window;
import net.gudenau.cavegame.renderer.shader.Shader;
import net.gudenau.cavegame.renderer.texture.Texture;
import net.gudenau.cavegame.renderer.vk.VkRenderer;
import net.gudenau.cavegame.resource.Identifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public final class VkDrawContext implements DrawContext {
    @NotNull
    private final VkRenderer renderer;
    @NotNull
    private final Shader shader;
    @NotNull
    private final Window.ResizeCallback resizeCallback;
    @NotNull
    private final Stack<VkScissorEntry> scissorStack = new Stack<>();
    @NotNull
    private final Map<Texture, BufferBuilder> builders = new HashMap<>();

    public VkDrawContext(VkRenderer renderer, Shader shader) {
        this.renderer = renderer;
        this.shader = shader;
        var window = renderer.window();

        resizeCallback = window.addResizeCallback((_, _, _) -> {
            throw new IllegalStateException(
                "The current scissor implementation is apparently invalid. The resize callback was fired when a screen was being drawn."
            );
        });

        var size = window.size();
        scissorStack.push(new VkScissorEntry(this, 0, 0, size.width(), size.height()));
    }

    @Override
    public int width() {
        return scissor().width();
    }

    @Override
    public int height() {
        return scissor().height();
    }

    @NotNull
    public VkScissorEntry scissor() {
        return scissorStack.peek();
    }

    public void popScissor(VkScissorEntry scissor) {
        if(scissor() != scissor) {
            throw new AssertionError("Can not pop a scissor entry that isn't the head of the stack");
        }

        scissorStack.pop();
    }

    @Contract("_, _, _, _ -> new")
    @Override
    @NotNull
    public StackEntry scissor(int x, int y, int width, int height) {
        //TODO Validate args
        var entry = new VkScissorEntry(this, x, y, width, height);
        scissorStack.push(entry);
        return entry;
    }

    private BufferBuilder builder(@NotNull Texture texture) {
        return builders.computeIfAbsent(texture, (_) -> shader.builder());
    }

    @Override
    public void drawRectangle(int x, int y, int width, int height, int color) {

    }

    @Override
    public void drawText(int x, int y, int width, int height, @NotNull String text, @NotNull FontStyle style) {

    }

    @Override
    public void drawImage(@NotNull Identifier identifier, int x, int y, int width, int height, int u, int v, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        var textureManager = renderer.textureManager();
        var texture = textureManager.getTexture(identifier)
            .orElseGet(textureManager::missingTexture);
        var builder = builder(texture);

        float texU1 = u / (float) textureWidth;
        float texV1 = v / (float) textureHeight;
        float texU2 = (u + uWidth) / (float) textureWidth;
        float texV2 = (u + vHeight) / (float) textureHeight;

        { // First triangle
            // Upper left
            builder.position(x, y)
                .color(1, 1, 1, 1)
                .textureCoord(texU1, texV1)
                .next();

            // Top right
            builder.position(x + width, y)
                .color(1, 1, 1, 1)
                .textureCoord(texU2, texV1)
                .next();

            // Bottom right
            builder.position(x + width, y + height)
                .color(1, 1, 1, 1)
                .textureCoord(texU2, texV2)
                .next();
        }

        { // Second triangle
            // Bottom right
            builder.position(x + width, y + height)
                .color(1, 1, 1, 1)
                .textureCoord(texU2, texV2)
                .next();

            // Bottom left
            builder.position(x, y + height)
                .color(1, 1, 1, 1)
                .textureCoord(texU1, texV2)
                .next();

            // Upper left
            builder.position(x, y)
                .color(1, 1, 1, 1)
                .textureCoord(texU1, texV1)
                .next();
        }
    }

    @Override
    public void close() {
        renderer.window().removeResizeCallback(resizeCallback);
    }
}

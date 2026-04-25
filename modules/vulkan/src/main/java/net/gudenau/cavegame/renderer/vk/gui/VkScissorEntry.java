package net.gudenau.cavegame.renderer.vk.gui;

import net.gudenau.cavegame.gui.drawing.DrawContext;

public record VkScissorEntry(
    VkDrawContext context,
    int x, int y,
    int width, int height
) implements DrawContext.StackEntry {
    @Override
    public void close() {
        context.popScissor(this);
    }
}

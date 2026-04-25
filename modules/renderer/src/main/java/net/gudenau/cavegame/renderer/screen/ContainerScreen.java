package net.gudenau.cavegame.renderer.screen;

import net.gudenau.cavegame.gui.component.Container;
import net.gudenau.cavegame.gui.layout.LayoutEngine;
import net.gudenau.cavegame.renderer.Renderer;
import org.jetbrains.annotations.NotNull;

public final class ContainerScreen extends Container implements Screen {
    private static final int INVALID = -1;

    private int width = INVALID;
    private int height = INVALID;

    public ContainerScreen(@NotNull LayoutEngine layoutEngine) {
        super(layoutEngine);
    }

    @Override
    public void setSize(int width, int height) {
        if(width != this.width || height != this.height) {
            this.width = width;
            this.height = height;
            invalidate();
        }
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public void draw(@NotNull Renderer renderer) {
        try(var context = renderer.drawContext()) {
            draw(context);
        }
    }

    @Override
    public void close() {
        Screen.super.close();
    }
}

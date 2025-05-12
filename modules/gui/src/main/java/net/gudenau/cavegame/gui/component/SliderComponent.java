package net.gudenau.cavegame.gui.component;

import net.gudenau.cavegame.gui.drawing.DrawContext;
import net.gudenau.cavegame.gui.drawing.ThreeByThree;
import net.gudenau.cavegame.gui.input.InputAction;
import net.gudenau.cavegame.gui.input.MouseButton;
import net.gudenau.cavegame.gui.value.SeekableValue;
import net.gudenau.cavegame.resource.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class SliderComponent<T> implements Component {
    private static final int PADDING_WIDTH = 10;
    private static final int PADDING_HEIGHT = 9;
    private static final ThreeByThree GRAPHICS = new ThreeByThree(new Identifier("gui", "slider"), 11, 9, 4, 3);
    private static final Identifier HANDLE = new Identifier("gui", "slider_handle");

    @NotNull
    private final ValueComponent<T> child;
    @NotNull
    private final SeekableValue<T> value;
    private double position = Double.NaN;
    @Nullable
    private Component parent;

    public SliderComponent(@NotNull ValueComponent<T> child) {
        this.child = child;
        if(!(child.value() instanceof SeekableValue<T> value)) {
            throw new IllegalArgumentException("Sliders can not accept non-seekable values");
        }
        this.value = value;

        recalculatePosition();

        child.parent(this);
    }

    private void recalculatePosition() {
        var index = value.indexOf();
        var limit = value.valueCount();
        position = index / (limit - 1.0);
    }

    @Override
    public int width() {
        return child.metrics().width() + PADDING_WIDTH;
    }

    @Override
    public int height() {
        return child.metrics().height() + PADDING_HEIGHT;
    }

    @Override
    public void parent(@NotNull Component parent) {
        if(this.parent != null) {
            throw new IllegalStateException("Component " + this + " already had " + this.parent + " as a parent");
        }

        this.parent = parent;
    }

    @Override
    public void onClick(int x, int y, @NotNull MouseButton button) {
    }

    @Override
    public void onDrag(int x, int y, @NotNull MouseButton button, @NotNull InputAction action) {
    }

    @Override
    public void onScroll(int x, int y, int amount) {
        SeekableValue<T> val = value;
        val.increment(Integer.signum(amount));
        recalculatePosition();
    }

    @Override
    @NotNull
    public Optional<Component> parent() {
        return Optional.ofNullable(parent);
    }

    @Override
    public void draw(@NotNull DrawContext context) {
        var ctxWidth = context.width();
        var ctxHeight = context.height();

        try(var _ = context.scissor(
            0, (ctxHeight - 9) / 2,
            ctxWidth, 9
        )) {
            GRAPHICS.draw(context);
        }

        context.drawImage(
            HANDLE,
            4 + (int) ((ctxWidth - 11) * position), (ctxHeight - 9) / 2,
            3, 9,
            0, 0,
            3, 9
        );

        try(var _ = context.scissor(
            (ctxWidth - child.width()) / 2,
            (ctxHeight - child.height()) / 2,
            child.width(),
            child.height()
        )) {
            child.draw(context);
        }
    }
}

package net.gudenau.cavegame.gui.component;

import net.gudenau.cavegame.gui.drawing.DrawContext;
import net.gudenau.cavegame.gui.MouseButton;
import net.gudenau.cavegame.gui.layout.Layout;
import net.gudenau.cavegame.gui.layout.LayoutEngine;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.SequencedSet;

public final class Container implements Component {
    private final SequencedSet<Component> children = new LinkedHashSet<>();
    private final LayoutEngine layoutEngine;

    @Nullable
    private Component parent;
    @Nullable
    private Layout layout;

    public Container(@NotNull LayoutEngine layoutEngine) {
        this.layoutEngine = layoutEngine;
    }

    @Override
    public void draw(@NotNull DrawContext context) {
        var layout = layout();
        context.drawRectangle(0, 0, layout.width(), layout.height(), 0xFF00FF00);
        int color = 0;
        for(var entry : layout.components()) {
            try(var _ = context.scissor(entry.x(), entry.y(), entry.width(), entry.height())) {
                context.drawRectangle(0, 0, context.width(), context.height(), 0xFF000000 | (0xFF << (color * 8)));
                color = (color + 1) % 3;
                entry.component().draw(context);
            }
        }
    }

    @NotNull
    private Optional<Layout.Entry> offsetEntryAt(int x, int y) {
        return layout().components()
            .stream()
            .filter((entry) ->
                x >= entry.x() &&
                y >= entry.y() &&
                x < entry.x() + entry.width() &&
                y < entry.y() + entry.height()
            )
            .findFirst()
            .map((entry) -> new Layout.Entry(
                x - entry.x(),
                y - entry.y(),
                entry.component()
            ));
    }

    @Override
    public void onClick(int x, int y, @NotNull MouseButton button) {
        offsetEntryAt(x, y)
            .ifPresent((entry) -> entry.component().onClick(entry.x(), entry.y(), button));
    }

    @Override
    public void onScroll(int x, int y, int amount) {
        offsetEntryAt(x, y)
            .ifPresent((entry) -> entry.component().onScroll(entry.x(), entry.y(), amount));
    }

    @Override
    public void invalidate() {
        Component.super.invalidate();

        layout = null;
    }

    @NotNull
    private Layout layout() {
        if(layout == null) {
            layout = layoutEngine.layout(this, children);
        }
        return layout;
    }

    @Override
    public int width() {
        return layout().width();
    }

    @Override
    public int height() {
        return layout().height();
    }

    @Contract("_ -> param1")
    @NotNull
    public <T extends Component> T add(@NotNull T component) {
        component.parent(this);

        if(children.add(component)) {
            layout = null;
        }
        return component;
    }

    @Override
    public void parent(@NotNull Component parent) {
        if(this.parent != null) {
            throw new IllegalStateException("Component " + this + " already had " + this.parent + " as a parent");
        }

        this.parent = parent;
    }

    @Override
    @NotNull
    public Optional<Component> parent() {
        return Optional.ofNullable(parent);
    }
}

package net.gudenau.cavegame.gui.component;

import net.gudenau.cavegame.gui.drawing.DrawContext;
import net.gudenau.cavegame.gui.input.InputAction;
import net.gudenau.cavegame.gui.input.MouseButton;
import net.gudenau.cavegame.gui.layout.Layout;
import net.gudenau.cavegame.gui.layout.LayoutEngine;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/// A container of other components.
public class Container implements Component {
    private final SequencedSet<Component> children = new LinkedHashSet<>();
    private final LayoutEngine layoutEngine;

    @Nullable
    private Component parent;
    @Nullable
    private Layout layout;

    @Nullable
    private Component dragTarget;
    private boolean isDragging;

    /// Constructs a new container with the provided {@link LayoutEngine}
    ///
    /// @param layoutEngine The {@link LayoutEngine} use to lay this container out
    public Container(@NotNull LayoutEngine layoutEngine) {
        this.layoutEngine = layoutEngine;
    }

    /// Get a read-only view of the children of this Container. If the children are modified this collection will
    /// reflect the changes.
    ///
    /// @return A read-only view of the children
    @NotNull
    public Collection<Component> children() {
        return Collections.unmodifiableCollection(children);
    }

    @Override
    public void draw(@NotNull DrawContext context) {
        var layout = layout();
        context.drawRectangle(0, 0, width(), height(), 0xFF00FF00);
        for(var entry : layout.components()) {
            try(var _ = context.scissor(entry.x(), entry.y(), entry.width(), entry.height())) {
                entry.component().draw(context);
            }
        }
    }

    /// Gets the {@link Layout.Entry Layout entry} that corresponds to a point in this container, if it exists.
    @NotNull
    private Optional<Layout.Entry> entryAt(int x, int y) {
        return layout().components()
            .stream()
            .filter((entry) ->
                x >= entry.x() &&
                    y >= entry.y() &&
                    x < entry.x() + entry.width() &&
                    y < entry.y() + entry.height()
            )
            .findFirst();
    }

    @NotNull
    private Optional<Layout.Entry> offsetEntryAt(int x, int y) {
        return entryAt(x, y)
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
    public void onDrag(int x, int y, @NotNull MouseButton button, @NotNull InputAction action) {
        if(action == InputAction.START) {
            offsetEntryAt(x, y).ifPresentOrElse((entry) -> {
                dragTarget = entry.component();
                dragTarget.onDrag(entry.x(), entry.y(), button, action);
            }, () -> {
                dragTarget = null;
            });
        } else {
            if(dragTarget == null) {
                return;
            }

            var entry = layout().entryOf(dragTarget);
            dragTarget.onDrag(x - entry.x(), y - entry.y(), button, action);

            if(action == InputAction.STOP) {
                dragTarget = null;
            }
        }
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
            layout = layoutEngine.layout(this);
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

    /// Adds a child to this container and sets the parent of the added component to this container.
    ///
    /// @param component The component to add
    /// @param <T> The type of the component
    /// @return The passed component
    /// @throws IllegalStateException If the provided component already has a parent
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

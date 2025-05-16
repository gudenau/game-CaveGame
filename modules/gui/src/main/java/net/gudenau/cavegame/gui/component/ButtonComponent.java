package net.gudenau.cavegame.gui.component;

import net.gudenau.cavegame.gui.drawing.DrawContext;
import net.gudenau.cavegame.gui.input.MouseButton;
import net.gudenau.cavegame.gui.drawing.ThreeByThree;
import net.gudenau.cavegame.resource.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/// A simple button component.
public final class ButtonComponent<T extends Component> implements Component {
    private static final int PADDING = 4;
    private static final ThreeByThree GRAPHICS = new ThreeByThree(new Identifier("gui", "button"), 9, PADDING);

    @NotNull
    private final T child;
    @Nullable
    private Runnable action;
    @Nullable
    private Component parent;

    /// Creates a new button component with the provided child for the button contents.
    ///
    /// @param child The child of this button
    public ButtonComponent(@NotNull T child) {
        this.child = child;
        child.parent(this);
    }

    /// Gets the child of this button.
    ///
    /// @return The child of this button
    @NotNull
    public T child() {
        return child;
    }

    /// Sets the click action of this button.
    ///
    /// @param action The new button action
    public void action(@NotNull Runnable action) {
        this.action = action;
    }

    @Override
    public void onClick(int x, int y, @NotNull MouseButton button) {
        if(action != null) {
            action.run();
        }
    }

    @Override
    public int width() {
        return child.width() + PADDING * 2;
    }

    @Override
    public int height() {
        return child.height() + PADDING * 2;
    }

    @Override
    public void draw(@NotNull DrawContext context) {
        var ctxWidth = context.width();
        var ctxHeight = context.height();

        //context.drawRectangle(0, 0, ctxWidth, ctxHeight, 0xFFFF00FF);
        GRAPHICS.draw(context);

        try(var _ = context.scissor(PADDING, PADDING, ctxWidth - PADDING * 2, ctxHeight - PADDING * 2)) {
            child.draw(context);
        }
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

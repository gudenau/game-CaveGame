package net.gudenau.cavegame.gui.component;

import net.gudenau.cavegame.gui.drawing.DrawContext;
import net.gudenau.cavegame.gui.MouseButton;
import net.gudenau.cavegame.gui.drawing.ThreeByThree;
import net.gudenau.cavegame.resource.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class ButtonComponent<T extends Component> implements Component {
    private static final ThreeByThree GRAPHICS = new ThreeByThree(new Identifier("gui", "button"), 9, 4);
    private static final int PADDING = GRAPHICS.border();

    @NotNull
    private final T child;
    @Nullable
    private Runnable action;
    @Nullable
    private Component parent;

    public ButtonComponent(@NotNull T child) {
        this.child = child;
        child.parent(this);
    }

    @NotNull
    public T child() {
        return child;
    }

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
        //context.drawRectangle(0, 0, width(), height(), 0xFFFF00FF);
        GRAPHICS.draw(context);

        try(var _ = context.scissor(PADDING, PADDING, width() - PADDING * 2, height() - PADDING * 2)) {
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

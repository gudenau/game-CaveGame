package net.gudenau.cavegame.gui.component;

import net.gudenau.cavegame.gui.Graphics;
import net.gudenau.cavegame.gui.MouseButton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class ButtonComponent<T extends Component> implements Component {
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
        return child.width();
    }

    @Override
    public int height() {
        return child.height();
    }

    @Override
    public void render(@NotNull Graphics graphics) {
        child.render(graphics);
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

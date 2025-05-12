package net.gudenau.cavegame.gui.component;

import net.gudenau.cavegame.gui.input.InputAction;
import net.gudenau.cavegame.gui.input.MouseButton;
import net.gudenau.cavegame.gui.drawing.Drawable;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface Component extends Drawable {
    int width();
    int height();

    void parent(@NotNull Component container);
    @NotNull Optional<Component> parent();

    default void onClick(int x, int y, @NotNull MouseButton button) {}

    default void onDrag(int x, int y, @NotNull MouseButton button, @NotNull InputAction action) {}

    default void onScroll(int x, int y, int amount) {}

    default void invalidate() {
        parent().ifPresent(Component::invalidate);
    }
}

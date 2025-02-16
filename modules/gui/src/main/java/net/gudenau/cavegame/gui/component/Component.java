package net.gudenau.cavegame.gui.component;

import net.gudenau.cavegame.gui.MouseButton;
import net.gudenau.cavegame.gui.drawing.Drawable;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface Component extends Drawable {
    int width();
    int height();

    void parent(@NotNull Component container);
    @NotNull Optional<Component> parent();

    default void onClick(int x, int y, @NotNull MouseButton button) {}

    default void onScroll(int x, int y, int amount) {}

    default void invalidate() {
        parent().ifPresent(Component::invalidate);
    }
}

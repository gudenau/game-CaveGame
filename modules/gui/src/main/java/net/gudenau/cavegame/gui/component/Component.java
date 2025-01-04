package net.gudenau.cavegame.gui.component;

import net.gudenau.cavegame.gui.Graphics;
import net.gudenau.cavegame.gui.MouseButton;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface Component {
    int width();
    int height();

    void render(@NotNull Graphics graphics);

    void parent(@NotNull Component container);
    @NotNull Optional<Component> parent();

    default void onClick(int x, int y, @NotNull MouseButton button) {
        System.out.println("Component " + this + " clicked at " + x + ", " + y);
    }

    default void invalidate() {
        parent().ifPresent(Component::invalidate);
    }
}

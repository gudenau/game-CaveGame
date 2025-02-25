package net.gudenau.cavegame.gui.layout;

import net.gudenau.cavegame.gui.component.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface LayoutEngine {
    @NotNull Layout layout(@NotNull Component parent, @NotNull Collection<Component> children);
}

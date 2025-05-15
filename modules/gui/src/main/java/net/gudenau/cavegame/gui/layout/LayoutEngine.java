package net.gudenau.cavegame.gui.layout;

import net.gudenau.cavegame.gui.component.Container;
import org.jetbrains.annotations.NotNull;

public interface LayoutEngine {
    void minimumSize(int width, int height);

    @NotNull Layout layout(@NotNull Container container);
}

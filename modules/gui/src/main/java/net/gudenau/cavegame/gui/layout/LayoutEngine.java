package net.gudenau.cavegame.gui.layout;

import net.gudenau.cavegame.gui.component.Container;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/// Responsible for the layouts of containers.
//FIXME Make this immutable
public interface LayoutEngine {
    /// Sets the minimum size that this layout engine needs to generate.
    ///
    /// @param width The minimum width
    /// @param height The minimum height
    void minimumSize(int width, int height);

    /// Lays out the children of the provided container and returns a new layout.
    ///
    /// @param container The container to layout
    /// @return The generated layout
    @Contract(value = "_ -> new", pure = true)
    @NotNull Layout layout(@NotNull Container container);
}

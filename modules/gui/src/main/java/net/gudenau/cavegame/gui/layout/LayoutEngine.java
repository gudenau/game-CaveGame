package net.gudenau.cavegame.gui.layout;

import net.gudenau.cavegame.gui.component.Container;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/// Responsible for the layouts of containers.
public interface LayoutEngine {
    /// Lays out the children of the provided container and returns a new layout.
    ///
    /// @param container The container to layout
    /// @return The generated layout
    @Contract(value = "_ -> new", pure = true)
    @NotNull Layout layout(@NotNull Container container);

    abstract class Builder<T extends LayoutEngine> {
        protected int width = -1;
        protected int height = -1;

        @Contract("_, _ -> this")
        @NotNull
        public final Builder<T> minimumSize(int width, int height) {
            this.width = width;
            this.height = height;

            return this;
        }

        @NotNull public abstract T build();
    }
}

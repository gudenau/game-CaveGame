package net.gudenau.cavegame.gui.layout;

import net.gudenau.cavegame.gui.component.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.SequencedCollection;

public record Layout(
    int width,
    int height,
    SequencedCollection<Entry> components
) {
    public record Entry(
        int x,
        int y,
        int width,
        int height,
        @NotNull Component component
    ) {
        public Entry(int x, int y, @NotNull Component component) {
            this(x, y, component.width(), component.height(), component);
        }
    }

    @NotNull
    public Entry entryOf(@Nullable Component child) {
        return components.stream()
            .filter((entry) -> entry.component == child)
            .findAny()
            .orElseThrow(() -> new IllegalStateException("Component " + child + " was not a child of this layout"));
    }
}

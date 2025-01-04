package net.gudenau.cavegame.gui.layout;

import net.gudenau.cavegame.gui.component.Component;
import org.jetbrains.annotations.NotNull;

import java.util.SequencedCollection;

public record Layout(
    int width,
    int height,
    SequencedCollection<Entry> components
) {
    public record Entry(
        int x,
        int y,
        @NotNull Component component
    ) {
        public int width() {
            return component.width();
        }

        public int height() {
            return component.height();
        }
    }
}

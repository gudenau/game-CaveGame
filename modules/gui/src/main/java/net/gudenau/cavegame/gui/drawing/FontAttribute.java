package net.gudenau.cavegame.gui.drawing;

import org.jetbrains.annotations.NotNull;

public sealed interface FontAttribute {
    enum Alignment {
        NEG, CENTER, POS,
    }

    record HorizontalAlignment(@NotNull Alignment alignment) implements FontAttribute {}

    @NotNull
    static HorizontalAlignment horizontalAlignment(@NotNull Alignment alignment) {
        return new HorizontalAlignment(alignment);
    }

    record VerticalAlignment(@NotNull Alignment alignment) implements FontAttribute {}

    @NotNull
    static VerticalAlignment verticalAlignment(@NotNull Alignment alignment) {
        return new VerticalAlignment(alignment);
    }

    record Color(int color) implements FontAttribute {}

    @NotNull
    static Color color(int color) {
        return new Color(color);
    }

    @NotNull
    static Color color(int red, int green, int blue) {
        return new Color(0xFF000000 | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF));
    }

    @NotNull
    static Color color(int red, int green, int blue, int alpha) {
        return new Color((alpha << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF));
    }
}

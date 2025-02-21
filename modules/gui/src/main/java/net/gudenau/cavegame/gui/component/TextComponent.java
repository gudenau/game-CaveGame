package net.gudenau.cavegame.gui.component;

import net.gudenau.cavegame.gui.drawing.DrawContext;
import net.gudenau.cavegame.gui.drawing.Font;
import net.gudenau.cavegame.gui.drawing.TextMetrics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;

public sealed class TextComponent implements Component permits ValueComponent {
    public enum Style {
        CENTER_HORIZONTAL,
    }

    @NotNull
    private final Font font;
    @NotNull
    private final EnumSet<Style> style;
    @NotNull
    private String text;
    @Nullable
    private TextMetrics metrics;
    @Nullable
    private Component parent;

    public TextComponent(@NotNull String text, @NotNull Font font, @NotNull Style @NotNull ... style) {
        this.text = text;
        this.font = font;
        this.style = EnumSet.noneOf(Style.class);
        Collections.addAll(this.style, style);
    }

    public void text(@NotNull String text) {
        if(this.text.equals(text)) {
            return;
        }

        this.text = text;
        invalidate();
    }

    @Override
    public void invalidate() {
        Component.super.invalidate();

        metrics = null;
    }

    @NotNull
    protected TextMetrics metrics() {
        if(metrics != null) {
            return metrics;
        }

        return metrics = font.metrics(text());
    }

    @NotNull
    protected TextMetrics metrics(@NotNull String text) {
        return font.metrics(text);
    }

    @Override
    public int width() {
        return metrics().width();
    }

    @Override
    public int height() {
        return metrics().height();
    }

    @NotNull
    protected String text() {
        return text;
    }

    @Override
    public void draw(@NotNull DrawContext context) {
        int x = this.style.contains(Style.CENTER_HORIZONTAL) ?
            (context.width() - width()) / 2 :
            0;

        context.drawText(x, metrics().ascent(), text(), 0xFF000000);
    }

    @Override
    public void parent(@NotNull Component parent) {
        if(this.parent != null) {
            throw new IllegalStateException("Component " + this + " already had " + this.parent + " as a parent");
        }

        this.parent = parent;
    }

    @Override
    @NotNull
    public Optional<Component> parent() {
        return Optional.ofNullable(parent);
    }
}

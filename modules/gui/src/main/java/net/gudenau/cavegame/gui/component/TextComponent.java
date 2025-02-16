package net.gudenau.cavegame.gui.component;

import net.gudenau.cavegame.gui.drawing.DrawContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Graphics2D;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;

//FIXME Remove AWT
public final class TextComponent implements Component {
    private record Metrics(int ascent, int width, int height) {}

    public enum Style {
        CENTER_HORIZONTAL,
    }

    @NotNull
    private final Graphics2D graphics;
    @NotNull
    private final EnumSet<Style> style;
    @NotNull
    private String text;
    @Nullable
    private Metrics metrics;
    @Nullable
    private Component parent;

    public TextComponent(@NotNull String text, @NotNull Graphics2D graphics, @NotNull Style @NotNull ... style) {
        this.text = text;
        this.graphics = graphics;
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
    private Metrics metrics() {
        if(metrics != null) {
            return metrics;
        }

        if(text.isBlank()) {
            return metrics = new Metrics(0, 0, 0);
        }

        var lines = text.lines()
            .map((line) -> graphics.getFontMetrics().getStringBounds(line, graphics))
            .toList();

        int width = 0;
        int height = 0;
        for(var line : lines) {
            width = Math.max(width, (int) line.getWidth());
            height += (int) line.getHeight();
        }

        return metrics = new Metrics(graphics.getFontMetrics().getAscent(), width, height);
    }

    @Override
    public int width() {
        return metrics().width();
    }

    @Override
    public int height() {
        return metrics().height();
    }

    @Override
    public void draw(@NotNull DrawContext context) {
        int x = this.style.contains(Style.CENTER_HORIZONTAL) ?
            (context.width() - width()) / 2 :
            0;

        context.drawText(x, metrics().ascent(), text, 0xFF000000);
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

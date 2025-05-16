package net.gudenau.cavegame.gui.component;

import net.gudenau.cavegame.gui.drawing.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;

/// A basic text based {@link Component}.
public sealed class TextComponent implements Component permits ValueComponent {
    @NotNull
    private final Font font;
    @NotNull
    private final FontStyle style;
    @NotNull
    private String text;
    @Nullable
    private TextMetrics metrics;
    @Nullable
    private Component parent;

    /// Constructs a new TextComponent.
    ///
    /// @param text The text to be rendered
    /// @param font The font to use for rendering
    /// @param style The style to use for rendering
    public TextComponent(@NotNull String text, @NotNull Font font, @NotNull FontStyle style) {
        this.text = text;
        this.font = font;
        this.style = style;
    }

    /// Updates the text of this component. Invalidates this component if the text is different.
    ///
    /// @param text The new text for this value
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

    /// Gets or computes and caches the {@link TextMetrics} for this component.
    ///
    /// @return The metrics for the current text
    @NotNull
    protected TextMetrics metrics() {
        if(metrics != null) {
            return metrics;
        }

        return metrics = font.metrics(text());
    }

    /// Calculates the {@link TextMetrics} for given text using the current font.
    ///
    /// @return The {@link TextMetrics} for the provided text
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

    /// Gets the current text of this component.
    ///
    /// @return The current text
    @NotNull
    protected String text() {
        return text;
    }

    @Override
    public void draw(@NotNull DrawContext context) {
        context.drawText(0, metrics().ascent(), context.width(), context.height(), text(), style);
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

package net.gudenau.cavegame.gui.component;

import net.gudenau.cavegame.gui.drawing.Font;
import net.gudenau.cavegame.gui.drawing.FontStyle;
import net.gudenau.cavegame.gui.drawing.TextMetrics;
import net.gudenau.cavegame.gui.value.UniverseValue;
import net.gudenau.cavegame.gui.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// A simple text component that is backed by a {@link Value}.
public final class ValueComponent<T> extends TextComponent {
    @NotNull
    private final Value<T> value;
    @Nullable
    private TextMetrics universeMetrics = null;

    /// Constructs a new ValueComponent with the provided value, font and style.
    ///
    /// @param value The value for the text of this component
    /// @param font The font to use to render this component
    /// @param style The style to use to render this component
    public ValueComponent(@NotNull Value<T> value, @NotNull Font font, @NotNull FontStyle style) {
        super("This text should never be seen", font, style);

        this.value = value;
        value.registerEvent((_, _) -> invalidate());

        if(value instanceof UniverseValue<T> universe) {
            universeMetrics = universe.metrics(font)
                .orElse(null);
        }
    }

    /// Gets the {@link Value} for this component.
    @NotNull
    public Value<T> value() {
        return value;
    }

    @Override
    public void text(@NotNull String text) {
        throw new UnsupportedOperationException("Can not set text on a ValueComponent");
    }

    @Override
    @NotNull
    protected String text() {
        return String.valueOf(value.value());
    }

    @Override
    @NotNull
    protected TextMetrics metrics() {
        return universeMetrics == null ? super.metrics() : universeMetrics;
    }
}

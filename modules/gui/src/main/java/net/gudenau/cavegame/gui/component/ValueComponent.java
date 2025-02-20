package net.gudenau.cavegame.gui.component;

import net.gudenau.cavegame.gui.value.UniverseValue;
import net.gudenau.cavegame.gui.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Graphics2D;

public final class ValueComponent<T> extends TextComponent {
    @NotNull
    private final Value<T> value;
    @Nullable
    private Metrics universeMetrics = null;

    public ValueComponent(@NotNull Value<T> value, @NotNull Graphics2D graphics, @NotNull Style @NotNull ... style) {
        super("This text should never be seen", graphics, style);

        this.value = value;
        value.registerEvent((_, _) -> invalidate());

        if(value instanceof UniverseValue<T> universe) {
            universeMetrics = universe.universe().stream()
                .map(String::valueOf)
                .map(this::metrics)
                .reduce((a, b) -> new Metrics(
                    Math.max(a.ascent(), b.ascent()),
                    Math.max(a.width(), b.width()),
                    Math.max(a.height(), b.height())
                ))
                .orElse(null);
        }
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
    protected Metrics metrics() {
        return universeMetrics == null ? super.metrics() : universeMetrics;
    }
}

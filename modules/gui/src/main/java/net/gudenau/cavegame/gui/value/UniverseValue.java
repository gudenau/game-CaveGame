package net.gudenau.cavegame.gui.value;

import net.gudenau.cavegame.gui.drawing.Font;
import net.gudenau.cavegame.gui.drawing.TextMetrics;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.SequencedCollection;

public sealed interface UniverseValue<T> extends SeekableValue<T> permits EnumValue {
    @NotNull SequencedCollection<T> universe();

    @Override
    @NotNull
    default T first() {
        return universe().getFirst();
    }

    @Override
    @NotNull
    default T last() {
        return universe().getLast();
    }

    @NotNull
    default T next() {
        var value = value();
        var universe = universe();
        var iterator = universe.iterator();
        while(iterator.hasNext()) {
            if(iterator.next().equals(value)) {
                break;
            }
        }

        var next = iterator.hasNext() ? iterator.next() : universe.getFirst();
        value(next);
        return next;
    }

    @NotNull
    default T previous() {
        var value = value();
        var universe = universe().reversed();
        var iterator = universe.iterator();
        while(iterator.hasNext()) {
            if(iterator.next().equals(value)) {
                break;
            }
        }

        var next = iterator.hasNext() ? iterator.next() : universe.getFirst();
        value(next);
        return next;
    }

    @NotNull
    default Optional<TextMetrics> metrics(@NotNull Font font) {
        return universe().stream()
            .map(String::valueOf)
            .map(font::metrics)
            .reduce((a, b) -> new TextMetrics(
                a.ascent(),
                Math.max(a.width(), b.width()),
                Math.max(a.height(), b.height())
            ));
    }
}

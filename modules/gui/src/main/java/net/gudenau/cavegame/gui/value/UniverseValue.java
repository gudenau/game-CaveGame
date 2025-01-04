package net.gudenau.cavegame.gui.value;

import org.jetbrains.annotations.NotNull;

import java.util.SequencedCollection;

public sealed interface UniverseValue<T> extends Value<T> permits EnumValue {
    @NotNull SequencedCollection<T> universe();

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
}

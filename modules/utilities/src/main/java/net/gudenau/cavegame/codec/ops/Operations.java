package net.gudenau.cavegame.codec.ops;

import java.util.Map;
import java.util.stream.Stream;

public interface Operations<T> extends FromOperations<T>, ToOperations<T> {
    T blank();

    <R> R convert(Operations<R> other, T input);

    T createList(Stream<T> stream);

    T createMap(Stream<Map.Entry<T, T>> stream);

    default <R> R convertList(Operations<R> other, T input) {
        return other.createList(toStream(input).result()
            .orElse(Stream.empty())
            .map((item) -> convert(other, item))
        );
    }

    default <R> R convertMap(Operations<R> other, T input) {
        return other.createMap(toEntryStream(input).result()
            .orElse(Stream.empty())
            .map((item) -> Map.entry(convert(other, item.getKey()), convert(other, item.getValue())))
        );
    }
}

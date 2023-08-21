package net.gudenau.cavegame.codec;

import net.gudenau.cavegame.codec.ops.Operations;

public interface Encoder<T> {
    <R> CodecResult<R> encode(Operations<R> operations, T input, R prefix);

    default  <R> CodecResult<R> encode(Operations<R> operations, T input) {
        return encode(operations, input, operations.blank());
    }
}

package net.gudenau.cavegame.codec;

import net.gudenau.cavegame.codec.ops.Operations;

public interface Decoder<T> {
    <R> CodecResult<T> decode(Operations<R> operations, R input);
}

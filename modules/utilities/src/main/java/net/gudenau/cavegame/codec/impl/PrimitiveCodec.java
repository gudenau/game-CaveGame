package net.gudenau.cavegame.codec.impl;

import net.gudenau.cavegame.codec.Codec;
import net.gudenau.cavegame.codec.CodecResult;
import net.gudenau.cavegame.codec.ops.Operations;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public interface PrimitiveCodec<T> extends Codec<T> {
    @Override
    default <R> CodecResult<R> encode(Operations<R> operations, T input, R prefix) {
        if(!Objects.equals(operations.blank(), prefix)) {
            throw new IllegalArgumentException("Primitives can't have partials");
        }
        return encode(operations, input);
    }

    @Override
    <R> CodecResult<R> encode(Operations<R> operations, T input);

    PrimitiveCodec<Byte> BYTE = new PrimitiveCodec<>() {
        @Override
        @NotNull
        public Class<Byte> type() {
            return Byte.class;
        }

        @Override
        public <R> CodecResult<R> encode(Operations<R> operations, Byte input) {
            return CodecResult.success(operations.fromByte(input));
        }

        @Override
        public <R> CodecResult<Byte> decode(Operations<R> operations, R input) {
            return operations.toByte(input);
        }
    };

    PrimitiveCodec<Short> SHORT = new PrimitiveCodec<>() {
        @Override
        @NotNull
        public Class<Short> type() {
            return Short.class;
        }

        @Override
        public <R> CodecResult<R> encode(Operations<R> operations, Short input) {
            return CodecResult.success(operations.fromShort(input));
        }

        @Override
        public <R> CodecResult<Short> decode(Operations<R> operations, R input) {
            return operations.toShort(input);
        }
    };

    PrimitiveCodec<Integer> INT = new PrimitiveCodec<>() {
        @Override
        @NotNull
        public Class<Integer> type() {
            return Integer.class;
        }

        @Override
        public <R> CodecResult<R> encode(Operations<R> operations, Integer input) {
            return CodecResult.success(operations.fromInt(input));
        }

        @Override
        public <R> CodecResult<Integer> decode(Operations<R> operations, R input) {
            return operations.toInt(input);
        }
    };
    
    PrimitiveCodec<Long> LONG = new PrimitiveCodec<>() {
        @Override
        @NotNull
        public Class<Long> type() {
            return Long.class;
        }

        @Override
        public <R> CodecResult<R> encode(Operations<R> operations, Long input) {
            return CodecResult.success(operations.fromLong(input));
        }

        @Override
        public <R> CodecResult<Long> decode(Operations<R> operations, R input) {
            return operations.toLong(input);
        }
    };
    
    PrimitiveCodec<Float> FLOAT = new PrimitiveCodec<>() {
        @Override
        @NotNull
        public Class<Float> type() {
            return Float.class;
        }

        @Override
        public <R> CodecResult<R> encode(Operations<R> operations, Float input) {
            return CodecResult.success(operations.fromFloat(input));
        }

        @Override
        public <R> CodecResult<Float> decode(Operations<R> operations, R input) {
            return operations.toFloat(input);
        }
    };

    PrimitiveCodec<Double> DOUBLE = new PrimitiveCodec<>() {
        @Override
        @NotNull
        public Class<Double> type() {
            return Double.class;
        }

        @Override
        public <R> CodecResult<R> encode(Operations<R> operations, Double input) {
            return CodecResult.success(operations.fromDouble(input));
        }

        @Override
        public <R> CodecResult<Double> decode(Operations<R> operations, R input) {
            return operations.toDouble(input);
        }
    };

    PrimitiveCodec<String> STRING = new PrimitiveCodec<>() {
        @Override
        @NotNull
        public Class<String> type() {
            return String.class;
        }

        @Override
        public <R> CodecResult<R> encode(Operations<R> operations, String input) {
            return CodecResult.success(operations.fromString(input));
        }

        @Override
        public <R> CodecResult<String> decode(Operations<R> operations, R input) {
            return operations.toString(input);
        }
    };
}

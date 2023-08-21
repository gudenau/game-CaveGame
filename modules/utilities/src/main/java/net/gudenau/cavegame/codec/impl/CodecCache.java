package net.gudenau.cavegame.codec.impl;

import net.gudenau.cavegame.codec.Codec;
import net.gudenau.cavegame.codec.CodecBuilder;
import net.gudenau.cavegame.util.Treachery;
import net.gudenau.cavegame.util.collection.SharedSet;
import net.gudenau.cavegame.util.collection.SharedSoftMap;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CodecCache {
    private static final Map<Class<?>, Codec<?>> CACHE = new SharedSoftMap<>();
    static {
        // Preload the primitives
        CACHE.put(byte.class, Codec.BYTE);
        CACHE.put(Byte.class, Codec.BYTE);
        CACHE.put(short.class, Codec.SHORT);
        CACHE.put(Short.class, Codec.SHORT);
        CACHE.put(int.class, Codec.INT);
        CACHE.put(Integer.class, Codec.INT);
        CACHE.put(long.class, Codec.LONG);
        CACHE.put(Long.class, Codec.LONG);
        CACHE.put(float.class, Codec.FLOAT);
        CACHE.put(Float.class, Codec.FLOAT);
        CACHE.put(double.class, Codec.DOUBLE);
        CACHE.put(Double.class, Codec.DOUBLE);
        CACHE.put(String.class, Codec.STRING);
    }

    private static final Set<Class<?>> GENERATING = SharedSet.hashSet();

    private CodecCache() {
        throw new AssertionError();
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<Codec<T>> get(Class<T> type) {
        return Optional.ofNullable((Codec<T>) CACHE.get(type));
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T> Codec<T> find(Class<T> type) {
        var codec = CACHE.get(type);
        if(codec != null) {
            return (Codec<T>) codec;
        }

        if(!GENERATING.add(type)) {
            throw new IllegalStateException("Class " + Treachery.longClassName(type) + " already has a CODEC being generated!");
        }
        try {
            codec = create(type);
            var existing = CACHE.putIfAbsent(type, codec);
            return (Codec<T>) (existing == null ? codec : existing);
        } finally {
            GENERATING.remove(type);
        }
    }

    @SuppressWarnings({"unchecked", "SingleStatementInBlock", "rawtypes"})
    private static <T> Codec<T> create(Class<T> type) {
        if(type.isRecord()) {
            return (Codec<T>) CodecBuilder.record((Class<? extends Record>) type);
        } else if(type.isEnum()) {
            return (Codec<T>) EnumCodec.of((Class) type);
        } else {
            throw new IllegalStateException("Don't know how to convert " + Treachery.longClassName(type) + " into a CODEC");
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Codec<T> put(Codec<T> codec) {
        var existing = CACHE.putIfAbsent(codec.type(), codec);
        return existing == null ? codec : (Codec<T>) existing;
    }
}

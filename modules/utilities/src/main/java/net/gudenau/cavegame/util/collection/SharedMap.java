package net.gudenau.cavegame.util.collection;

import net.gudenau.cavegame.util.SharedLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class SharedMap<K, V> implements Map<K, V> {
    private final SharedLock lock = new SharedLock();
    private final Map<K, V> map;

    private SharedMap(Map<K, V> map) {
        this.map = map;
    }

    @NotNull
    public static <K, V> SharedMap<K, V> of(@NotNull Map<K, V> map) {
        if(map instanceof SharedMap<K,V> shared) {
            return shared;
        } else {
            return new SharedMap<>(map);
        }
    }

    @Override
    public int size() {
        return lock.read(map::size);
    }

    @Override
    public boolean isEmpty() {
        return lock.read(map::isEmpty);
    }

    @Override
    public boolean containsKey(Object key) {
        return lock.read(() -> map.containsKey(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return lock.read(() -> map.containsValue(value));
    }

    @Override
    public V get(Object key) {
        return lock.read(() -> map.get(key));
    }

    @Nullable
    @Override
    public V put(K key, V value) {
        return lock.write(() -> map.put(key, value));
    }

    @Override
    public V remove(Object key) {
        return lock.write(() -> map.remove(key));
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        lock.write(() -> map.putAll(m));
    }

    @Override
    public void clear() {
        lock.write(map::clear);
    }

    private WeakReference<SharedSet<K>> keySet;

    @NotNull
    @Override
    public Set<K> keySet() {
        return lock.readWrite(
            () -> keySet == null ? null : keySet.get(),
            () -> {
                var value = SharedSet.of(map.keySet());
                keySet = new WeakReference<>(value);
                return value;
            }
        );
    }

    private WeakReference<SharedCollection<V>> values;

    @NotNull
    @Override
    public Collection<V> values() {
        return lock.readWrite(
            () -> values == null ? null : values.get(),
            () -> {
                var value = SharedCollection.of(map.values());
                values = new WeakReference<>(value);
                return value;
            }
        );
    }

    private WeakReference<SharedSet<Entry<K, V>>> entrySet;

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return lock.readWrite(
            () -> entrySet == null ? null : entrySet.get(),
            () -> {
                var value = SharedSet.of(map.entrySet());
                entrySet = new WeakReference<>(value);
                return value;
            }
        );
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return lock.read(() -> map.getOrDefault(key, defaultValue));
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        lock.read(() -> map.forEach(action));
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        lock.write(() -> map.replaceAll(function));
    }

    @Nullable
    @Override
    public V putIfAbsent(K key, V value) {
        V existing = get(key);
        if(existing == null) {
            return lock.write(() -> map.putIfAbsent(key, value));
        }
        return existing;
    }

    @Override
    public boolean remove(Object key, Object value) {
        var current = get(key);
        if (!Objects.equals(current, value) || (current == null && !containsKey(key))) {
            return false;
        }
        return lock.write(() -> map.remove(key, value));
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        var curValue = get(key);
        if (!Objects.equals(curValue, oldValue) || (curValue == null && !containsKey(key))) {
            return false;
        }
        return lock.write(() -> map.replace(key, oldValue, newValue));
    }

    @Nullable
    @Override
    public V replace(K key, V value) {
        var state = new Object() {
            V current;
            boolean modify;
        };
        lock.read(() -> {
            state.current = map.get(key);
            state.modify = state.current != null || containsKey(key);
        });
        if(state.modify) {
            return lock.write(() -> map.replace(key, value));
        } else {
            return state.current;
        }
    }

    @Override
    public V computeIfAbsent(K key, @NotNull Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        V oldValue = get(key);
        if (oldValue == null) {
            return lock.write(() -> map.computeIfAbsent(key, mappingFunction));
        }
        return oldValue;
    }

    @Override
    public V computeIfPresent(K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        V oldValue = get(key);
        if (oldValue != null) {
            return lock.write(() -> map.computeIfPresent(key, remappingFunction));
        }
        return null;
    }

    @Override
    public V compute(K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return lock.write(() -> map.compute(key, remappingFunction));
    }

    @Override
    public V merge(K key, @NotNull V value, @NotNull BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return lock.write(() -> map.merge(key, value, remappingFunction));
    }
}

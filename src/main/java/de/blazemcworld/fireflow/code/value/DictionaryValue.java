package de.blazemcworld.fireflow.code.value;

import de.blazemcworld.fireflow.code.type.WireType;

import java.util.*;

public class DictionaryValue<K, V> {

    public final WireType<K> keyType;
    public final WireType<V> valueType;
    private final Map<Wrap<K>, Wrap<V>> store = new HashMap<>();

    public DictionaryValue(WireType<K> keyType, WireType<V> valueType) {
        this.keyType = keyType;
        this.valueType = valueType;
    }

    public DictionaryValue(WireType<K> keyType, WireType<V> valueType, Map<K, V> store) {
        this.keyType = keyType;
        this.valueType = valueType;
        for (Map.Entry<K, V> entry : store.entrySet()) {
            this.store.put(new Wrap<>(keyType, entry.getKey()), new Wrap<>(valueType, entry.getValue()));
        }
    }

    public V get(K key) {
        Wrap<K> keyWrap = new Wrap<>(keyType, key);
        if (!store.containsKey(keyWrap)) return valueType.defaultValue();
        V out = store.get(keyWrap).value;
        if (out == null) return valueType.defaultValue();
        return out;
    }

    public int size() {
        return store.size();
    }

    public List<K> keys() {
        List<K> out = new ArrayList<>();
        for (Wrap<K> key : store.keySet()) {
            out.add(key.value);
        }
        return out;
    }

    public boolean containsKey(K k) {
        return store.containsKey(new Wrap<>(keyType, k));
    }

    public DictionaryValue<K, V> put(K key, V value) {
        DictionaryValue<K, V> updated = copy();
        updated.store.put(new Wrap<>(keyType, key), new Wrap<>(valueType, value));
        return updated;
    }

    public DictionaryValue<K,V> remove(K key) {
        DictionaryValue<K, V> updated = copy();
        updated.store.remove(new Wrap<>(keyType, key));
        return updated;
    }

    private DictionaryValue<K, V> copy() {
        DictionaryValue<K, V> out = new DictionaryValue<>(keyType, valueType);
        out.store.putAll(store);
        return out;
    }

    public boolean has(K key) {
        return store.containsKey(new Wrap<>(keyType, key));
    }

    public static class Wrap<T> {
        private final WireType<T> type;
        private final T value;

        public Wrap(WireType<T> type, T value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        @SuppressWarnings({"unchecked"})
        public boolean equals(Object obj) {
            if (obj instanceof DictionaryValue.Wrap<?> other && other.type == type) {
                return type.valuesEqual(value, (T) other.value);
            }
            return false;
        }
    }
}

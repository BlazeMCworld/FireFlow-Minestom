package de.blazemcworld.fireflow.code.type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import de.blazemcworld.fireflow.code.value.DictionaryValue;
import de.blazemcworld.fireflow.util.Translations;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.item.Material;

import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;

public class DictionaryType<K, V> extends WireType<DictionaryValue<K, V>> {

    public static final DictionaryType<?, ?> UNSPECIFIED = new DictionaryType<>(null, null);
    public final WireType<K> keyType;
    public final WireType<V> valueType;
    private static final WeakHashMap<WireType<?>, WeakHashMap<WireType<?>, DictionaryType<?, ?>>> instances = new WeakHashMap<>();

    private DictionaryType(WireType<K> keyType, WireType<V> valueType) {
        super("dictionary", computeColor(keyType, valueType), Material.CHISELED_BOOKSHELF);
        this.keyType = keyType;
        this.valueType = valueType;
    }

    @SuppressWarnings("unchecked")
    public static <K, V> DictionaryType<K, V> of(WireType<K> keyType, WireType<V> valueType) {
        return (DictionaryType<K, V>) instances.computeIfAbsent(keyType, k -> new WeakHashMap<>()).computeIfAbsent(valueType, v -> new DictionaryType<>(keyType, valueType));
    }

    @Override
    public DictionaryValue<K, V> defaultValue() {
        return new DictionaryValue<>(keyType, valueType);
    }

    private static TextColor computeColor(WireType<?> keyType, WireType<?> valueType) {
        if (keyType == null || valueType == null) return NamedTextColor.WHITE;
        int r = (keyType.color.red() + valueType.color.red()) / 2;
        int g = (keyType.color.green() + valueType.color.green()) / 2;
        int b = (keyType.color.blue() + valueType.color.blue()) / 2;
        return TextColor.color(r, g, b);
    }

    @Override
    @SuppressWarnings("unchecked")
    public DictionaryValue<K, V> checkType(Object obj) {
        if (obj instanceof DictionaryValue<?, ?> dict && dict.keyType == keyType && dict.valueType == valueType) return (DictionaryValue<K, V>) dict;
        return null;
    }
    
    @Override
    public int getTypeCount() {
        return 2;
    }

    @Override
    public List<WireType<?>> getTypes() {
        return List.of(keyType, valueType);
    }

    @Override
    public WireType<?> withTypes(List<WireType<?>> types) {
        return DictionaryType.of(types.get(0), types.get(1));
    }

    @Override
    public boolean acceptsType(WireType<?> type, int index) {
        return AllTypes.isValue(type);
    }

    @Override
    public String getName() {
        if (keyType == null || valueType == null) return Translations.get("type.dictionary");
        return Translations.get("type.typed_dictionary", keyType.getName(), valueType.getName());
    }

    @Override
    protected String stringifyInternal(DictionaryValue<K, V> value) {
        return value.keyType.getName() + " -> " + value.valueType.getName() + " x" + value.size();
    }

    @Override
    public JsonElement toJson(DictionaryValue<K, V> obj) {
        JsonArray out = new JsonArray();
        for (K key : obj.keys()) {
            JsonArray array = new JsonArray();
            array.add(keyType.toJson(key));
            array.add(valueType.toJson(obj.get(key)));
            out.add(array);
        }
        return out;
    }

    @Override
    public DictionaryValue<K, V> fromJson(JsonElement json) {
        HashMap<K, V> store = new HashMap<>();
        for (JsonElement elem : json.getAsJsonArray()) {
            JsonArray array = elem.getAsJsonArray();
            store.put(keyType.fromJson(array.get(0)), valueType.fromJson(array.get(1)));
        }
        return new DictionaryValue<>(keyType, valueType, store);
    }

    @Override
    public boolean valuesEqual(DictionaryValue<K, V> a, DictionaryValue<K, V> b) {
        if (a.size() != b.size()) return false;
        if (a.keyType != b.keyType) return false;
        if (a.valueType != b.valueType) return false;
        for (K k : a.keys()) {
            if (!b.containsKey(k)) return false;
            if (!a.valueType.valuesEqual(a.get(k), b.get(k))) return false;
        }
        return true;
    }
}

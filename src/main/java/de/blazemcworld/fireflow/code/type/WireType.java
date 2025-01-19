package de.blazemcworld.fireflow.code.type;

import com.google.gson.JsonElement;
import de.blazemcworld.fireflow.util.Translations;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.item.Material;

import java.util.List;

public abstract class WireType<T> {

    public final String id;
    public final TextColor color;
    public final Material icon;

    public WireType(String id, TextColor color, Material icon) {
        this.id = id;
        this.color = color;
        this.icon = icon;
    }

    public abstract T defaultValue();
    public abstract T checkType(Object obj);
    public abstract JsonElement toJson(T obj);
    public abstract T fromJson(JsonElement json);
    public abstract boolean valuesEqual(T a, T b);

    public JsonElement convertToJson(Object obj) {
        return toJson(checkType(obj));
    }

    public T parseInset(String str) {
        return null;
    }

    public int getTypeCount() {
        return 0;
    }

    public List<WireType<?>> getTypes() {
        return List.of(this);
    }

    public WireType<?> withTypes(List<WireType<?>> types) {
        return null;
    }

    public boolean acceptsType(WireType<?> type, int index) {
        return false;
    }

    public String getName() {
        return Translations.get("type." + id);
    }

    public String stringify(Object value) {
        return stringifyInternal(checkType(value));
    }

    protected abstract String stringifyInternal(T value);

    public T convert(WireType<?> other, Object v) {
        T checked = checkType(v);
        return checked != null ? checked : defaultValue();
    }

    public boolean canConvert(WireType<?> other) {
        return false;
    }
}

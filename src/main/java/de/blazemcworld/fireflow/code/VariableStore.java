package de.blazemcworld.fireflow.code;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.blazemcworld.fireflow.code.type.AllTypes;
import de.blazemcworld.fireflow.code.type.WireType;

import java.util.*;
import java.util.function.Predicate;

public class VariableStore {

    private final HashMap<String, Object> values = new HashMap<>();
    private final HashMap<String, WireType<?>> types = new HashMap<>();

    public <T> T get(String name, WireType<T> type) {
        T out = type.checkType(values.get(name));
        if (out == null) out = type.defaultValue();
        return out;
    }

    public <T> void set(String name, WireType<T> type, T value) {
        values.put(name, value);
        types.put(name, type);
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();

        JsonArray types = new JsonArray();
        List<WireType<?>> typesList = List.copyOf(Set.copyOf(this.types.values()));
        for (WireType<?> type : typesList) {
            types.add(AllTypes.toJson(type));
        }
        obj.add("types", types);

        JsonArray values = new JsonArray();
        for (String name : this.values.keySet()) {
            JsonArray entry = new JsonArray();
            entry.add(name);
            WireType<?> type = this.types.get(name);
            Object value = this.values.get(name);
            entry.add(typesList.indexOf(type));
            entry.add(type.convertToJson(value));
            values.add(entry);
        }
        obj.add("values", values);

        return obj;
    }

    public void load(JsonObject obj) {
        JsonArray types = obj.getAsJsonArray("types");
        List<WireType<?>> typesList = new ArrayList<>();
        for (JsonElement typeElem : types) {
            typesList.add(AllTypes.fromJson(typeElem));
        }
        for (JsonElement entry : obj.getAsJsonArray("values")) {
            String name = entry.getAsJsonArray().get(0).getAsString();
            int typeIndex = entry.getAsJsonArray().get(1).getAsInt();
            JsonElement value = entry.getAsJsonArray().get(2);
            WireType<?> type = typesList.get(typeIndex);
            this.types.put(name, type);
            this.values.put(name, type.fromJson(value));
        }
    }

    public Set<VarEntry> iterator(Predicate<String> filter, int limit) {
        Set<VarEntry> out = new HashSet<>();
        for (String name : this.values.keySet()) {
            if (!filter.test(name)) continue;
            WireType<?> type = this.types.get(name);
            out.add(new VarEntry(name, type, this.values.get(name)));
            if (out.size() >= limit) break;
        }
        return out;
    }

    public void reset() {
        values.clear();
        types.clear();
    }

    public record VarEntry(String name, WireType<?> type, Object value) {}
}

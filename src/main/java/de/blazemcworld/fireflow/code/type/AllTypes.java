package de.blazemcworld.fireflow.code.type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AllTypes {

    public static final List<WireType<?>> all = new ArrayList<>();

    public static void init() {
        all.add(AnyType.INSTANCE);
        all.add(ConditionType.INSTANCE);
        all.add(DictionaryType.UNSPECIFIED);
        all.add(EntityType.INSTANCE);
        all.add(ItemType.INSTANCE);
        all.add(ListType.UNSPECIFIED);
        all.add(NumberType.INSTANCE);
        all.add(ParticleType.INSTANCE);
        all.add(PlayerType.INSTANCE);
        all.add(PositionType.INSTANCE);
        all.add(SignalType.INSTANCE);
        all.add(StringType.INSTANCE);
        all.add(TextType.INSTANCE);
        all.add(VectorType.INSTANCE);

        all.sort(Comparator.comparing(WireType::getName));
    }

    public static boolean isValue(WireType<?> type) {
        return type != SignalType.INSTANCE;
    }

    public static WireType<?> fromJson(JsonElement json) {
        if (json.isJsonPrimitive()) {
            for (WireType<?> t : all) {
                if (t.id.equals(json.getAsString())) {
                    return t;
                }
            }
            return null;
        }

        JsonArray array = json.getAsJsonArray();
        WireType<?> main = null;
        for (WireType<?> t : all) {
            if (t.id.equals(array.get(0).getAsString())) {
                main = t;
                break;
            }
        }
        if (main == null) return null;

        List<WireType<?>> subTypes = new ArrayList<>();
        for (int i = 1; i < array.size(); i++) {
            subTypes.add(fromJson(array.get(i)));
        }

        return main.withTypes(subTypes);
    }

    public static JsonElement toJson(WireType<?> type) {
        if (type.getTypeCount() == 0) {
            return new JsonPrimitive(type.id);
        }
        
        JsonArray json = new JsonArray();

        json.add(new JsonPrimitive(type.id));
        for (WireType<?> t : type.getTypes()) {
            json.add(AllTypes.toJson(t));
        }

        return json;
    }

}

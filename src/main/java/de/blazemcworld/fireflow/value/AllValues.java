package de.blazemcworld.fireflow.value;

import de.blazemcworld.fireflow.compiler.StructDefinition;
import net.minestom.server.network.NetworkBuffer;

import java.util.ArrayList;
import java.util.List;

public class AllValues {

    public static final List<Value> dataOnly = List.of(
            ConditionValue.INSTANCE,
            ListValue.get(NumberValue.INSTANCE),
            MessageValue.INSTANCE,
            NumberValue.INSTANCE,
            PlayerValue.INSTANCE,
            TextValue.INSTANCE
    );

    public static final List<Value> any = new ArrayList<>(dataOnly.size());
    static {
        any.add(SignalValue.INSTANCE);
        any.addAll(dataOnly);
    }

    public static List<Value> any(List<StructDefinition> structs) {
        List<Value> list = new ArrayList<>(structs.size() + any.size());
        list.addAll(any);
        for (StructDefinition st : structs) list.add(st.type);
        return list;
    }

    public static List<Value> dataOnly(List<StructDefinition> structs) {
        List<Value> list = new ArrayList<>(structs.size() + dataOnly.size());
        list.addAll(dataOnly);
        for (StructDefinition st : structs) list.add(st.type);
        return list;
    }

    public static Value get(String name) {
        for (Value value : any) {
            if (value.getBaseName().equals(name)) return value;
        }
        return null;
    }

    public static void writeValue(NetworkBuffer buffer, Value value) {
        buffer.write(NetworkBuffer.STRING, value.getBaseName());
        List<Value> generics = value.toGenerics();
        buffer.write(NetworkBuffer.INT, generics.size());
        for (Value generic : generics) {
            writeValue(buffer, generic);
        }
    }

    public static Value readValue(NetworkBuffer buffer) {
        String name = buffer.read(NetworkBuffer.STRING);
        List<Value> generics = new ArrayList<>();
        int genericsSize = buffer.read(NetworkBuffer.INT);
        for (int i = 0; i < genericsSize; i++) {
            generics.add(readValue(buffer));
        }
        Value norm = AllValues.get(name);
        if (norm == null) return null;
        return norm.fromGenerics(generics);
    }


}

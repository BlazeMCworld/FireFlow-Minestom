package de.blazemcworld.fireflow.node.impl.dictionary;

import de.blazemcworld.fireflow.node.Node;
import de.blazemcworld.fireflow.node.annotation.FlowSignalInput;
import de.blazemcworld.fireflow.node.annotation.FlowSignalOutput;
import de.blazemcworld.fireflow.node.annotation.FlowValueInput;
import de.blazemcworld.fireflow.value.AllValues;
import de.blazemcworld.fireflow.value.DictionaryValue;
import de.blazemcworld.fireflow.value.SignalValue;
import de.blazemcworld.fireflow.value.Value;

import java.util.List;
import java.util.Map;

public class DictionarySetNode extends Node {

    private final Value key;
    private final Value value;

    public DictionarySetNode(Value key, Value value) {
        super("Dictionary<" + key.getFullName() + ", " + value.getFullName() + "> Set");
        this.key = key;
        this.value = value;

        input("Signal", SignalValue.INSTANCE);
        input("Dictionary", DictionaryValue.get(key, value));
        input("Key", key);
        input("Value", value);
        output("Next", SignalValue.INSTANCE);

        loadJava(DictionarySetNode.class);
    }

    @FlowSignalOutput("Next")
    private static void next() {
        throw new IllegalStateException();
    }

    @FlowSignalInput("Signal")
    private static void signal() {
        dictionary().put(key(), value());
        next();
    }

    @FlowValueInput("Dictionary")
    private static Map<Object, Object> dictionary() {
        throw new IllegalStateException();
    }

    @FlowValueInput("Key")
    private static Object key() {
        throw new IllegalStateException();
    }

    @FlowValueInput("Value")
    private static Object value() {
        throw new IllegalStateException();
    }

    @Override
    public String getBaseName() {
        return "Dictionary Set";
    }

    @Override
    public List<Value> generics() {
        return List.of(key, value);
    }

    @Override
    public Node fromGenerics(List<Value> generics) {
        return new DictionarySetNode(generics.getFirst(), generics.get(1));
    }

    @Override
    public List<List<Value>> possibleGenerics() {
        return List.of(AllValues.dataOnly, AllValues.dataOnly);
    }
}

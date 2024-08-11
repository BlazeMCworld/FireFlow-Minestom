package de.blazemcworld.fireflow.node;

import de.blazemcworld.fireflow.compiler.FunctionDefinition;
import de.blazemcworld.fireflow.editor.CodeEditor;
import de.blazemcworld.fireflow.editor.widget.GenericSelectorWidget;
import de.blazemcworld.fireflow.node.impl.AddNumbersNode;
import de.blazemcworld.fireflow.node.impl.WhileNode;
import de.blazemcworld.fireflow.node.impl.event.PlayerJoinEvent;
import de.blazemcworld.fireflow.node.impl.extraction.list.ListSizeNode;
import de.blazemcworld.fireflow.node.impl.extraction.player.PlayerUUIDNode;
import de.blazemcworld.fireflow.node.impl.extraction.struct.StructFieldNode;
import de.blazemcworld.fireflow.node.impl.extraction.text.TextToMessageNode;
import de.blazemcworld.fireflow.node.impl.player.SendMessageNode;
import de.blazemcworld.fireflow.node.impl.variable.*;
import de.blazemcworld.fireflow.value.*;
import it.unimi.dsi.fastutil.Pair;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public record NodeCategory(String name, NodesSupplier supplier) {

    private static Entry setVarNodeEntry(VariableScope scope, CodeEditor e, Vec o) {
        return Entry.of("Set " + scope.getName() + " Variable", cb -> new GenericSelectorWidget(o, "Variable Type", AllValues.dataOnly, e, (type) -> {
            cb.accept(new SetVariableNode(scope, type));
        }));
    }

    private static Entry getVarNodeEntry(VariableScope scope, CodeEditor e, Vec o) {
        return Entry.of("Get " + scope.getName() + " Variable", cb -> new GenericSelectorWidget(o, "Variable Type", AllValues.dataOnly, e, (type) -> {
            cb.accept(new GetVariableNode(scope, type));
        }));
    }

    public final static NodeCategory EVENTS = new NodeCategory("Events", (e, o) -> List.of(
            Entry.of("On Player Join", cb -> cb.accept(new PlayerJoinEvent()))
    ));

    public final static NodeCategory NUMBERS = new NodeCategory("Numbers", (e, o) -> List.of(
            Entry.of("Addition", cb -> cb.accept(new AddNumbersNode()))
    ));

    public final static NodeCategory FLOW = new NodeCategory("Flow", (e, o) -> List.of(
            Entry.of("While Loop", cb -> cb.accept(new WhileNode()))
    ));

    public final static NodeCategory PLAYERS = new NodeCategory("Players", (e, o) -> List.of(
            Entry.of("Send Message", cb -> cb.accept(new SendMessageNode()))
    ));

    public final static NodeCategory FUNCTIONS = new NodeCategory("Functions", (e, o) -> {
        List<Entry> list = new ArrayList<>(e.functions.size());
        for (FunctionDefinition def : e.functions) list.add(Entry.of(def.fnName, cb -> cb.accept(def.createCall())));
        return list;
    });

    public final static NodeCategory VARIABLES = new NodeCategory("Variables", (e, o) -> List.of(
            setVarNodeEntry(LocalVariableScope.INSTANCE, e, o),
            getVarNodeEntry(LocalVariableScope.INSTANCE, e, o),
            setVarNodeEntry(SpaceVariableScope.INSTANCE, e, o),
            getVarNodeEntry(SpaceVariableScope.INSTANCE, e, o),
            setVarNodeEntry(PersistentVariableScope.INSTANCE, e, o),
            getVarNodeEntry(PersistentVariableScope.INSTANCE, e, o)
    ));

    public final static NodeCategory[] CATEGORIES = new NodeCategory[]{
            EVENTS,
            FUNCTIONS,
            VARIABLES,
            FLOW,
            NUMBERS,
            PLAYERS,
    };

    private final static HashMap<Value, NodeCategory> extractions = new HashMap<>();

    private static void putExtractions(Value type, NodesSupplier supplier) {
        extractions.putIfAbsent(type, new NodeCategory(type.getFullName() + " Extractions", supplier));
    }

    public static @Nullable NodeCategory getExtractions(Value type) {
        if (type instanceof ListValue) return getListExtractions((ListValue) type);
        else if (type instanceof StructValue) return getStructExtractions((StructValue) type);
        else return extractions.get(type);
    }

    private static NodeCategory getListExtractions(ListValue type) {
        String name = type.getFullName();
        return new NodeCategory(type.getFullName() + " Extractions", (e, o) -> List.of(
                Entry.of(name + " Size", cb -> cb.accept(new ListSizeNode(type)))
        ));
    }

    private static NodeCategory getStructExtractions(StructValue type) {
        return new NodeCategory(type.getBaseName() + " Extractions", (e, o) -> {
            ArrayList<Entry> list = new ArrayList<>(type.size());
            for (int i = 0; i < type.size(); i++) {
                final int finalI = i;
                Pair<String, Value> pair = type.getField(finalI);
                list.add(Entry.of(pair.left(), cb -> cb.accept(new StructFieldNode(type, finalI, pair))));
            }
            return list;
        });
    }

    static {
        putExtractions(PlayerValue.INSTANCE, (e, o) -> List.of(
                Entry.of("Get UUID", cb -> cb.accept(new PlayerUUIDNode()))
        ));
        putExtractions(TextValue.INSTANCE, (e, o) -> List.of(
                Entry.of("To Message", cb -> cb.accept(new TextToMessageNode()))
        ));
    }

    public interface NodesSupplier {
        List<Entry> get(CodeEditor e, Vec o);
    }

    public record Entry(String name, Consumer<Consumer<Node>> callback) {
        public static Entry of(String name, Consumer<Consumer<Node>> callback) {
            return new Entry(name, callback);
        }
    }
}

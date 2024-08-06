package de.blazemcworld.fireflow.node.impl.lists;

import de.blazemcworld.fireflow.compiler.instruction.DiscardInstruction;
import de.blazemcworld.fireflow.compiler.instruction.InstanceMethodInstruction;
import de.blazemcworld.fireflow.compiler.instruction.MultiInstruction;
import de.blazemcworld.fireflow.node.Node;
import de.blazemcworld.fireflow.node.NodeInput;
import de.blazemcworld.fireflow.node.NodeOutput;
import de.blazemcworld.fireflow.value.ListValue;
import de.blazemcworld.fireflow.value.SignalValue;
import de.blazemcworld.fireflow.value.Value;
import it.unimi.dsi.fastutil.Pair;
import org.objectweb.asm.Type;

import java.util.List;

public class ListAppendNode extends Node {

    public ListAppendNode(Value type) {
        super("Append to List");

        ListValue listType = ListValue.get(type);
        NodeInput signal = input("Signal", SignalValue.INSTANCE);
        NodeInput list = input("List", listType);
        NodeInput value = input("Value", type);
        NodeOutput next = output("Next", SignalValue.INSTANCE);

        signal.setInstruction(new MultiInstruction(
                Type.VOID_TYPE,
                new DiscardInstruction(new InstanceMethodInstruction(
                        List.class, list, "add", Type.BOOLEAN_TYPE,
                        List.of(Pair.of(Type.getType(Object.class), type.wrapPrimitive(value)))
                )),
                next
        ));
    }

}

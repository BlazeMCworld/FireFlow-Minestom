package de.blazemcworld.fireflow.node.impl.struct;

import de.blazemcworld.fireflow.compiler.instruction.MultiInstruction;
import de.blazemcworld.fireflow.compiler.instruction.RawInstruction;
import de.blazemcworld.fireflow.node.Node;
import de.blazemcworld.fireflow.node.NodeInput;
import de.blazemcworld.fireflow.value.StructValue;
import de.blazemcworld.fireflow.value.Value;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

public class UnpackStructNode extends Node {

    public UnpackStructNode(StructValue type) {
        super("Unpack " + type.getBaseName());
        NodeInput struct = input(type.getBaseName(), type);
        for (int i = 0; i < type.fields.size(); i++) {
            StructValue.Field field = type.fields.get(i);
            Value fieldType = field.type();
            output(field.name(), fieldType).setInstruction(new MultiInstruction(Type.getType("Ljava/lang/Object;"),
                    struct,
                    new RawInstruction(Type.VOID_TYPE, new LdcInsnNode(i)),
                    fieldType.cast(new RawInstruction(Type.VOID_TYPE, new InsnNode(Opcodes.AALOAD)))
            ));
        }
    }
}

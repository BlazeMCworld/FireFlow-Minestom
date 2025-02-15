package de.blazemcworld.fireflow.code.node.impl.vector;

import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.type.StringType;
import de.blazemcworld.fireflow.code.type.VectorType;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.Material;

public class RoundVectorAxesNode extends Node {

    public RoundVectorAxesNode() {
        super("round_vector_axes", Material.CLAY_BALL);

        Input<Vec> vector = new Input<>("vector", VectorType.INSTANCE);
        Input<String> mode = new Input<>("mode", StringType.INSTANCE).options("Round", "Floor", "Ceil");
        Output<Vec> rounded = new Output<>("rounded", VectorType.INSTANCE);

        rounded.valueFrom(ctx -> {
            Vec v = vector.getValue(ctx);
            switch (mode.getValue(ctx)) {
                case "Round" -> {
                    return v.apply(Vec.Operator.operator(Math::round));
                }
                case "Floor" -> {
                    return v.apply(Vec.Operator.FLOOR);
                }
                case "Ceil" -> {
                    return v.apply(Vec.Operator.CEIL);
                }
            }
            return v;
        });
    }

    @Override
    public Node copy() {
        return new RoundVectorAxesNode();
    }

}


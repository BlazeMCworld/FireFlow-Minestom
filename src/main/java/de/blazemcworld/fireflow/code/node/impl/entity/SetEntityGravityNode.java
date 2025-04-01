package de.blazemcworld.fireflow.code.node.impl.entity;

import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.type.ConditionType;
import de.blazemcworld.fireflow.code.type.EntityType;
import de.blazemcworld.fireflow.code.type.SignalType;
import de.blazemcworld.fireflow.code.value.EntityValue;
import net.minestom.server.item.Material;

public class SetEntityGravityNode extends Node {

    public SetEntityGravityNode() {
        super("set_entity_gravity", Material.SHULKER_SHELL);

        Input<Void> signal = new Input<>("signal", SignalType.INSTANCE);
        Input<EntityValue> entity = new Input<>("entity", EntityType.INSTANCE);
        Input<Boolean> enable = new Input<>("enable", ConditionType.INSTANCE);
        Output<Void> next = new Output<>("next", SignalType.INSTANCE);

        signal.onSignal((ctx) -> {
            entity.getValue(ctx).use(ctx, e -> e.setNoGravity(!enable.getValue(ctx)));
            ctx.sendSignal(next);
        });
    }

    @Override
    public Node copy() {
        return new SetEntityGravityNode();
    }
}

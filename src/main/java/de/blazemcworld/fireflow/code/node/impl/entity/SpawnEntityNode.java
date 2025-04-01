package de.blazemcworld.fireflow.code.node.impl.entity;

import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.type.EntityType;
import de.blazemcworld.fireflow.code.type.PositionType;
import de.blazemcworld.fireflow.code.type.SignalType;
import de.blazemcworld.fireflow.code.type.StringType;
import de.blazemcworld.fireflow.code.value.EntityValue;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.item.Material;

public class SpawnEntityNode extends Node {

    public SpawnEntityNode() {
        super("spawn_entity", Material.JUNGLE_SAPLING);

        Input<Void> signal = new Input<>("signal", SignalType.INSTANCE);
        Input<Pos> position = new Input<>("position", PositionType.INSTANCE);
        Input<String> type = new Input<>("type", StringType.INSTANCE);

        Output<Void> next = new Output<>("next", SignalType.INSTANCE);
        Output<EntityValue> entity = new Output<>("entity", EntityType.INSTANCE);
        entity.valueFromScope();

        signal.onSignal((ctx) -> {
            net.minestom.server.entity.EntityType t = net.minestom.server.entity.EntityType.fromKey(type.getValue(ctx));
            if (t == null) {
                ctx.setScopeValue(entity, EntityType.INSTANCE.defaultValue());
            } else {
                Entity e = new Entity(t);
                e.setInstance(ctx.evaluator.space.play, position.getValue(ctx));
                ctx.setScopeValue(entity, new EntityValue(e));
            }
            ctx.sendSignal(next);
        });
    }

    @Override
    public Node copy() {
        return new SpawnEntityNode();
    }
}
package de.blazemcworld.fireflow.code.node.impl.action.player;

import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.type.NumberType;
import de.blazemcworld.fireflow.code.type.PlayerType;
import de.blazemcworld.fireflow.code.type.SignalType;
import de.blazemcworld.fireflow.code.value.PlayerValue;
import net.minestom.server.item.Material;

public class SetPlayerHealthNode extends Node {
    public SetPlayerHealthNode() {
        super("set_player_health", Material.RED_DYE);
        Input<Void> signal = new Input<>("signal", SignalType.INSTANCE);
        Input<PlayerValue> player = new Input<>("player", PlayerType.INSTANCE);
        Input<Double> health = new Input<>("health", NumberType.INSTANCE);
        Output<Void> next = new Output<>("next", SignalType.INSTANCE);
        signal.onSignal((ctx) -> {
            player.getValue(ctx).tryUse(ctx, p -> p.setHealth(health.getValue(ctx).intValue()));
            ctx.sendSignal(next);
        });
    }

    @Override
    public Node copy() {
        return new SetPlayerHealthNode();
    }
}

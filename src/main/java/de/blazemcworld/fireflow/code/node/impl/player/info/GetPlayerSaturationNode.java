package de.blazemcworld.fireflow.code.node.impl.player.info;

import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.type.NumberType;
import de.blazemcworld.fireflow.code.type.PlayerType;
import de.blazemcworld.fireflow.code.value.PlayerValue;
import net.minestom.server.item.Material;

public class GetPlayerSaturationNode extends Node {
    public GetPlayerSaturationNode() {
        super("get_player_saturation", Material.GOLDEN_CARROT);
        Input<PlayerValue> player = new Input<>("player", PlayerType.INSTANCE);
        Node.Output<Double> saturation = new Output<>("saturation", NumberType.INSTANCE);

        saturation.valueFrom(ctx -> player.getValue(ctx).tryGet(ctx, p -> (double) p.getFoodSaturation(), 0.0));
    }

    @Override
    public Node copy() {
        return new GetPlayerSaturationNode();
    }
}

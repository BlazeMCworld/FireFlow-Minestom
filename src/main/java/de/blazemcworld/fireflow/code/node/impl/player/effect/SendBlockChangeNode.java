package de.blazemcworld.fireflow.code.node.impl.player.effect;

import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.type.PlayerType;
import de.blazemcworld.fireflow.code.type.PositionType;
import de.blazemcworld.fireflow.code.type.SignalType;
import de.blazemcworld.fireflow.code.type.StringType;
import de.blazemcworld.fireflow.code.value.PlayerValue;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.BlockChangePacket;

public class SendBlockChangeNode extends Node {
    public SendBlockChangeNode() {
        super("send_block_change", Material.AXOLOTL_BUCKET);
        Input<Void> signal = new Input<>("signal", SignalType.INSTANCE);
        Input<String> block = new Input<>("block", StringType.INSTANCE);
        Input<Pos> position = new Input<>("position", PositionType.INSTANCE);
        Input<PlayerValue> player = new Input<>("player", PlayerType.INSTANCE);
        Output<Void> next = new Output<>("next", SignalType.INSTANCE);
        signal.onSignal((ctx) -> {
            Block placedBlock = Block.fromKey(block.getValue(ctx));
            if (placedBlock != null) {
                player.getValue(ctx).tryUse(ctx, p -> p.sendPacket(new BlockChangePacket(position.getValue(ctx), placedBlock)));
            }
            ctx.sendSignal(next);
        });
    }

    @Override
    public Node copy() {
        return new SendBlockChangeNode();
    }
}
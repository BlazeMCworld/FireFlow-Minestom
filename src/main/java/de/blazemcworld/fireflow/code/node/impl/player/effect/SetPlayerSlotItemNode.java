package de.blazemcworld.fireflow.code.node.impl.player.effect;

import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.type.ItemType;
import de.blazemcworld.fireflow.code.type.NumberType;
import de.blazemcworld.fireflow.code.type.PlayerType;
import de.blazemcworld.fireflow.code.type.SignalType;
import de.blazemcworld.fireflow.code.value.PlayerValue;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public class SetPlayerSlotItemNode extends Node {

    public SetPlayerSlotItemNode() {
        super("set_player_slot_item", Material.SMOOTH_STONE);

        Input<Void> signal = new Input<>("signal", SignalType.INSTANCE);
        Input<PlayerValue> player = new Input<>("player", PlayerType.INSTANCE);
        Input<Double> slot = new Input<>("slot", NumberType.INSTANCE);
        Input<ItemStack> item = new Input<>("item", ItemType.INSTANCE);
        Output<Void> next = new Output<>("next", SignalType.INSTANCE);

        signal.onSignal((ctx) -> {
            player.getValue(ctx).tryUse(ctx, p -> {
                int s = slot.getValue(ctx).intValue();
                if (s < 0 || s >= p.getInventory().getSize()) return;
                p.getInventory().setItemStack(s, item.getValue(ctx));
            });
            ctx.sendSignal(next);
        });
    }

    @Override
    public Node copy() {
        return new SetPlayerSlotItemNode();
    }

}


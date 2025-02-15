package de.blazemcworld.fireflow.code.node.impl.player.effect;

import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.type.ItemType;
import de.blazemcworld.fireflow.code.type.PlayerType;
import de.blazemcworld.fireflow.code.type.SignalType;
import de.blazemcworld.fireflow.code.value.PlayerValue;
import net.minestom.server.inventory.TransactionOption;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public class TakePlayerItemNode extends Node {

    public TakePlayerItemNode() {
        super("take_player_item", Material.HOPPER_MINECART);

        Input<Void> signal = new Input<>("signal", SignalType.INSTANCE);
        Input<PlayerValue> player = new Input<>("player", PlayerType.INSTANCE);
        Input<ItemStack> item = new Input<>("item", ItemType.INSTANCE);
        Output<Void> next = new Output<>("next", SignalType.INSTANCE);

        signal.onSignal((ctx) -> {
            player.getValue(ctx).tryUse(ctx, p -> {
                p.getInventory().takeItemStack(item.getValue(ctx), TransactionOption.ALL);
            });
            ctx.sendSignal(next);
        });
    }

    @Override
    public Node copy() {
        return new TakePlayerItemNode();
    }

}


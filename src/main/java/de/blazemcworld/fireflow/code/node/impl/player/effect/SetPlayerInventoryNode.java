package de.blazemcworld.fireflow.code.node.impl.player.effect;

import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.type.*;
import de.blazemcworld.fireflow.code.value.ListValue;
import de.blazemcworld.fireflow.code.value.PlayerValue;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public class SetPlayerInventoryNode extends Node {

    public SetPlayerInventoryNode() {
        super("set_player_inventory", Material.WATER_BUCKET);

        Input<Void> signal = new Input<>("signal", SignalType.INSTANCE);
        Input<PlayerValue> player = new Input<>("player", PlayerType.INSTANCE);
        Input<ListValue<ItemStack>> contents = new Input<>("contents", ListType.of(ItemType.INSTANCE));
        Input<String> behaviour = new Input<>("behaviour", StringType.INSTANCE)
                .options("Clear", "Merge");
        Output<Void> next = new Output<>("next", SignalType.INSTANCE);

        signal.onSignal((ctx) -> {
            player.getValue(ctx).tryUse(ctx, p -> {
                PlayerInventory inv = p.getInventory();
                boolean clearInv = behaviour.getValue(ctx).equals("Clear");
                if (clearInv) inv.clear();

                ListValue<ItemStack> items = contents.getValue(ctx);
                int stop = Math.min(items.size(), inv.getSize());
                for (int slot = 0; slot < stop; slot++) {
                    ItemStack replacement = items.get(slot);
                    if (!clearInv && replacement.isAir()) continue;
                    inv.setItemStack(slot, replacement);
                }
            });
            ctx.sendSignal(next);
        });
    }

    @Override
    public Node copy() {
        return new SetPlayerInventoryNode();
    }

}



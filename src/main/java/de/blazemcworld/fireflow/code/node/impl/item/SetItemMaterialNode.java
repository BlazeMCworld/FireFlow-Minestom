package de.blazemcworld.fireflow.code.node.impl.item;

import de.blazemcworld.fireflow.code.node.Node;
import de.blazemcworld.fireflow.code.type.ItemType;
import de.blazemcworld.fireflow.code.type.StringType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public class SetItemMaterialNode extends Node {

    public SetItemMaterialNode() {
        super("set_item_material", Material.PAPER);

        Input<ItemStack> item = new Input<>("item", ItemType.INSTANCE);
        Input<String> material = new Input<>("material", StringType.INSTANCE);
        Output<ItemStack> updated = new Output<>("updated", ItemType.INSTANCE);

        updated.valueFrom((ctx) -> {
            Material mat = Material.fromKey(material.getValue(ctx));
            if (mat == null) mat = Material.AIR;
            return item.getValue(ctx).withMaterial(mat);
        });
    }

    @Override
    public Node copy() {
        return new SetItemMaterialNode();
    }
}

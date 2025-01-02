package de.blazemcworld.fireflow.code.type;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.NetworkBuffer;

import java.util.Base64;

public class ItemType extends WireType<ItemStack> {

    public static final ItemType INSTANCE = new ItemType();

    private ItemType() {
        super("item", NamedTextColor.GRAY, Material.ITEM_FRAME);
    }

    @Override
    public ItemStack defaultValue() {
        return ItemStack.AIR;
    }

    @Override
    public ItemStack convert(Object obj) {
        if (obj instanceof ItemStack item) return item;
        return null;
    }

    @Override
    public JsonElement toJson(ItemStack item) {
        NetworkBuffer buffer = NetworkBuffer.resizableBuffer();
        buffer.write(ItemStack.NETWORK_TYPE, item);
        return new JsonPrimitive(Base64.getEncoder().encodeToString(buffer.read(NetworkBuffer.RAW_BYTES)));
    }

    @Override
    public ItemStack fromJson(JsonElement json) {
        NetworkBuffer buffer = NetworkBuffer.resizableBuffer();
        buffer.write(NetworkBuffer.RAW_BYTES, Base64.getDecoder().decode(json.getAsString()));
        return buffer.read(ItemStack.NETWORK_TYPE);
    }

    @Override
    protected String stringifyInternal(ItemStack value) {
        return value.material().name() + " x" + value.amount();
    }
}

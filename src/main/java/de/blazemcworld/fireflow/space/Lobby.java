package de.blazemcworld.fireflow.space;

import de.blazemcworld.fireflow.inventory.ActiveSpacesInventory;
import de.blazemcworld.fireflow.inventory.MySpacesInventory;
import de.blazemcworld.fireflow.util.Statistics;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.player.PlayerUseItemOnBlockEvent;
import net.minestom.server.event.trait.BlockEvent;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.IChunkLoader;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public class Lobby {

    public static final InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();

    private static final ItemStack MY_SPACES = ItemStack.builder(Material.ENCHANTED_BOOK)
            .customName(Component.text("My Spaces").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false))
            .lore(
                    Component.text("Manage your spaces").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("using this item.").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            )
            .build();

    private static final ItemStack ACTIVE_SPACES = ItemStack.builder(Material.BLAZE_POWDER)
            .customName(Component.text("Active Spaces").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false))
            .lore(
                    Component.text("Manage your spaces").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("using this item.").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            )
            .build();

    public static void init() {
        instance.setTimeRate(0);
        instance.setChunkSupplier(LightingChunk::new);
        instance.setChunkLoader(IChunkLoader.noop());

        instance.setGenerator(unit -> {
            if (Math.abs(unit.absoluteStart().x() + 8) > 16) return;
            if (Math.abs(unit.absoluteStart().z() + 8) > 16) return;
            unit.modifier().fillHeight(-1, 0, Block.POLISHED_ANDESITE);
        });

        EventNode<InstanceEvent> events = instance.eventNode();

        events.addListener(PlayerMoveEvent.class, (event) -> {
            if (event.getNewPosition().y() < -20) {
                event.setNewPosition(Pos.ZERO);
            }
        });

        events.addListener(InstanceEvent.class, (event) -> {
            if (event instanceof BlockEvent && instance instanceof CancellableEvent cancelable) {
                cancelable.setCancelled(true);
            }
        });
        events.addListener(PlayerSwapItemEvent.class, (event) -> event.setCancelled(true));
        events.addListener(ItemDropEvent.class, (event) -> event.setCancelled(true));
        events.addListener(InventoryPreClickEvent.class, (event) -> {
            if (event.getInventory() instanceof PlayerInventory) event.setCancelled(true);
        });

        events.addListener(PlayerSpawnEvent.class, (event) -> {
            Statistics.reset(event.getPlayer());
            event.getPlayer().getInventory().setItemStack(0, MY_SPACES);
            event.getPlayer().getInventory().setItemStack(4, ACTIVE_SPACES);
        });

        events.addListener(PlayerUseItemEvent.class, (event) -> {
            event.setCancelled(true);
            rightClick(event.getPlayer());
        });

        events.addListener(PlayerUseItemOnBlockEvent.class, (event) -> {
            rightClick(event.getPlayer());
        });
    }

    private static void rightClick(Player player) {
        if (player.getItemInMainHand().isSimilar(MY_SPACES)) {
            MySpacesInventory.open(player);
        }
        if (player.getItemInMainHand().isSimilar(ACTIVE_SPACES)) {
            ActiveSpacesInventory.open(player);
        }
    }
}

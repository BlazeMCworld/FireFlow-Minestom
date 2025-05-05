package de.blazemcworld.fireflow.inventory;

import de.blazemcworld.fireflow.code.type.TextType;
import de.blazemcworld.fireflow.space.SpaceInfo;
import de.blazemcworld.fireflow.space.SpaceManager;
import de.blazemcworld.fireflow.util.Config;
import de.blazemcworld.fireflow.util.Transfer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MySpacesInventory {

    private static final ItemStack CREATE_SPACE = ItemStack.builder(Material.GREEN_STAINED_GLASS)
            .customName(Component.text("Create Space").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false))
            .lore(Component.text("Click to create a new space.").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
            .build();

    public static void open(Player player) {
        List<SpaceInfo> spaces = new ArrayList<>();

        Inventory inv = new Inventory(InventoryType.CHEST_3_ROW, "My Spaces") {
            @Override
            public boolean handleClick(@NotNull Player p, @NotNull Click click) {
                if (p != player) return false;

                if (click.slot() < spaces.size() && click.slot() >= 0) {
                    Transfer.move(player, SpaceManager.getOrLoadSpace(spaces.get(click.slot())).play);
                    return true;
                }

                if (click.slot() == 26 && spaces.size() < Config.store.limits().spacesPerPlayer() && SpaceManager.info.size() < Config.store.limits().totalSpaces()) {
                    SpaceInfo info = new SpaceInfo(SpaceManager.lastId++);
                    info.name = p.getUsername() + "'s New Space";
                    info.icon = Material.PAPER;
                    info.owner = p.getUuid();
                    info.developers = new HashSet<>();
                    info.builders = new HashSet<>();
                    SpaceManager.info.put(info.id, info);
                    MySpacesInventory.open(player);
                    return true;
                }
                return false;
            }
        };

        for (SpaceInfo space : SpaceManager.info.values()) {
            if (space.owner.equals(player.getUuid())) {
                spaces.add(space);
            }
        }

        for (int i = 0; i < spaces.size(); i++) {
            inv.setItemStack(i, ItemStack.builder(spaces.get(i).icon)
                    .customName(TextType.MM.deserialize(spaces.get(i).name).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
                    .lore(Component.text("ID: " + spaces.get(i).id).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                    .build());
        }

        if (spaces.size() < Config.store.limits().spacesPerPlayer() && SpaceManager.info.size() < Config.store.limits().totalSpaces()) {
            inv.setItemStack(26, CREATE_SPACE);
        }

        player.openInventory(inv);
    }

}

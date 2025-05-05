package de.blazemcworld.fireflow.inventory;

import de.blazemcworld.fireflow.code.type.TextType;
import de.blazemcworld.fireflow.space.Space;
import de.blazemcworld.fireflow.space.SpaceInfo;
import de.blazemcworld.fireflow.space.SpaceManager;
import de.blazemcworld.fireflow.util.Transfer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

public class ActiveSpacesInventory {

    public static void open(Player player) {
        List<Space> spaces = SpaceManager.activeSpaces();

        Inventory inv = new Inventory(InventoryType.CHEST_3_ROW, "Active Spaces") {
            @Override
            public boolean handleClick(@NotNull Player p, @NotNull Click click) {
                if (p != player) return false;
                if (click.slot() >= spaces.size()) return false;

                Transfer.move(player, SpaceManager.getOrLoadSpace(spaces.get(click.slot()).info).play);
                return true;
            }
        };

        spaces.sort(Comparator.comparingInt(s -> -s.play.getPlayers().size()));

        for (int i = 0; i < spaces.size(); i++) {
            SpaceInfo info = spaces.get(i).info;
            inv.setItemStack(i, ItemStack.builder(info.icon)
                    .customName(TextType.MM.deserialize(info.name).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
                    .lore(
                            Component.text("Players: " + spaces.get(i).play.getPlayers().size()).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                            Component.text("ID: " + info.id).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    )
                    .build());
        }

        player.openInventory(inv);
    }

}


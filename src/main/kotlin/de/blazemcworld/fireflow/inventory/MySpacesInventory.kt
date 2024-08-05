package de.blazemcworld.fireflow.inventory

import de.blazemcworld.fireflow.Config
import de.blazemcworld.fireflow.database.DatabaseHelper
import de.blazemcworld.fireflow.database.table.SpacesTable
import de.blazemcworld.fireflow.space.SpaceManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.Player
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.jetbrains.exposed.sql.transactions.transaction

object MySpacesInventory {

    private val NEW_SPACE_ITEM = ItemStack.builder(Material.GREEN_STAINED_GLASS)
        .customName(Component.text("New Space").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)).build()

    fun open(player: Player) {
        val inv = Inventory(InventoryType.CHEST_3_ROW,"My Spaces")

        val spaceIds = mutableListOf<Int>()
        transaction {
            val ownedSpaces = DatabaseHelper.ownedSpaces(player)
                .adjustSelect { select(SpacesTable.icon, SpacesTable.title, SpacesTable.id) }

            for (ownedSpace in ownedSpaces) {
                val id = ownedSpace[SpacesTable.id].value
                inv.setItemStack(spaceIds.size, ItemStack.builder(ownedSpace[SpacesTable.icon])
                    .customName(ownedSpace[SpacesTable.title].decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
                    .lore(Component.text("Space #$id").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                    .build())
                spaceIds += id
            }
        }

        inv.addInventoryCondition click@{ who, slot, _, _ ->
            if (player != who) return@click
            val id = spaceIds.getOrNull(slot) ?: return@click
            player.closeInventory()
            SpaceManager.sendToSpace(player, id)
        }

        if (spaceIds.size < Config.store.limits.spacesPerPlayer) {
            inv.setItemStack(8, NEW_SPACE_ITEM)
            inv.addInventoryCondition click@{ who, slot, _, _ ->
                if (player != who || slot != 8) return@click
                SpaceManager.createSpace(player) ?: return@click
                open(player)
            }
        }

        player.openInventory(inv)
    }

}
package de.blazemcworld.fireflow.inventory

import de.blazemcworld.fireflow.database.DatabaseHelper
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.EventFilter
import net.minestom.server.event.EventNode
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

object DeleteInventory {

    fun open(player: Player, callback: (Boolean) -> Unit) {
        val inv = Inventory(InventoryType.CHEST_1_ROW, "Delete 5+ nodes?")

        inv.setItemStack(3, ItemStack.builder(Material.EMERALD)
            .customName(Component.text("Yes").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GREEN))
            .lore(Component.text("WARNING: This will delete the selected nodes!").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.RED))
            .build())

        inv.setItemStack(5, ItemStack.builder(Material.REDSTONE)
            .customName(Component.text("No").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.RED))
            .build())

        inv.setItemStack(8, ItemStack.builder(Material.BARRIER)
            .customName(Component.text("Never ask me this again").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.DARK_RED))
            .lore(Component.text("WARNING: This will delete the selected nodes!").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.RED),
                  Component.text("Can be reverted in the preference menu").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY))
            .build())

        player.openInventory(inv)

        inv.addInventoryCondition { who, slot, type, result ->
            if (who !is Player) return@addInventoryCondition

            when (slot) {
                3 -> {
                    who.closeInventory()
                    callback(true)
                }
                5 -> {
                    who.closeInventory()
                    callback(false)
                }
                8 -> {
                    DatabaseHelper.updatePreferences(who, mapOf("delete-warning" to 1.toByte()))
                    who.closeInventory()
                    callback(true)
                }
            }
        }

        val handler = MinecraftServer.getGlobalEventHandler()
        val node = EventNode.type("closeInv", EventFilter.INVENTORY)

        node.addListener(InventoryCloseEvent::class.java) {
            if (it.inventory != inv) return@addListener
            callback(false)
            handler.removeChild(node)
        }

        handler.addChild(node)
    }
}
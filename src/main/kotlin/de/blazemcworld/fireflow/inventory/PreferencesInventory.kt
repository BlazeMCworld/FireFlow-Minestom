package de.blazemcworld.fireflow.inventory

import de.blazemcworld.fireflow.database.DatabaseHelper
import de.blazemcworld.fireflow.preferences.AutoToolsPreference
import de.blazemcworld.fireflow.preferences.DeleteWarningPreference
import de.blazemcworld.fireflow.preferences.MousePreference
import de.blazemcworld.fireflow.preferences.ReloadPreference
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.EventFilter
import net.minestom.server.event.EventNode
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.inventory.click.ClickType
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

object PreferencesInventory {

    private val preferences = mapOf(
        "reload" to ReloadPreference,
        "auto-tools" to AutoToolsPreference,
        "code-control" to MousePreference,
        "delete-warning" to DeleteWarningPreference
    )

    fun open(player: Player) {
        val preferenceItemMap = mutableMapOf<Int, String>()

        val inv = Inventory(InventoryType.CHEST_1_ROW,"Preferences")
        val knownPreferences = DatabaseHelper.preferences(player)

        var index = 0
        for ((key, value) in knownPreferences) {
            if (!preferences.containsKey(key)) continue
            inv.setItemStack(index, createItem(key, value))
            preferenceItemMap[index] = key
            index++
        }

        inv.addInventoryCondition click@{ who, slot, type, _ ->
            if (player != who || slot !in preferenceItemMap) return@click

            val key = preferenceItemMap[slot] ?: return@click
            if (!knownPreferences.containsKey(key)) return@click

            val pref = preferences[key] ?: return@click
            var knownPref = knownPreferences[key] ?: return@click
            knownPref = if (type == ClickType.RIGHT_CLICK) pref.decreaseState(knownPref) else pref.increaseState(knownPref)
            knownPreferences[key] = knownPref

            inv.setItemStack(slot, createItem(key, knownPref))
            return@click
        }

        player.openInventory(inv)

        val handler = MinecraftServer.getGlobalEventHandler()
        val node = EventNode.type("closeInv", EventFilter.INVENTORY)

        node.addListener(InventoryCloseEvent::class.java) {
            if (it.inventory != inv) return@addListener
            DatabaseHelper.updatePreferences(player, knownPreferences)
            MousePreference.playerPreference[player] = knownPreferences["code-control"]
            handler.removeChild(node)
        }

        handler.addChild(node)
    }

    private fun createItem(preference: String, value: Byte): ItemStack {
        val pref = preferences[preference] ?: return ItemStack.builder(Material.AIR).build()

        val lore = pref.getLore()
        lore[value.toInt()] = lore[value.toInt()].color(NamedTextColor.YELLOW)

        return ItemStack
            .builder(pref.getState(value).getIcon())
            .customName(pref.getName())
            .lore(lore)
            .build()
    }
}
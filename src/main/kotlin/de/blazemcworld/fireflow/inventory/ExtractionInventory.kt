package de.blazemcworld.fireflow.inventory

import de.blazemcworld.fireflow.node.TypeExtraction
import de.blazemcworld.fireflow.node.ValueType
import de.blazemcworld.fireflow.util.sendError
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.Player
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemStack

object ExtractionInventory {
    fun <T : Any> openForType(player: Player, type: ValueType<T>, callback: (TypeExtraction<T, *>) -> Unit) {
        val inv = Inventory(invForSize(type.extractions.size), "Extraction")
        val extractions = type.extractions

        if (extractions.isEmpty()) {
            player.sendError("No extractions available the type: ${type.name}.")
            return
        }

        for (i in extractions.indices) {
            inv.setItemStack(i, extractionItem(extractions[i]))
        }

        player.openInventory(inv)

        inv.addInventoryCondition { who, slot, _, _ ->
            if (who !is Player) return@addInventoryCondition
            val extraction = extractions.getOrNull(slot) ?: return@addInventoryCondition
            callback(extraction)
            who.closeInventory()
        }
    }

    fun extractionItem(extraction: TypeExtraction<*, *>): ItemStack {
        return ItemStack.builder(extraction.icon)
            .customName(Component.text(extraction.name).color(extraction.output.type.color).decoration(TextDecoration.ITALIC, false))
            .lore(Component.text(extraction.input.type.name).color(extraction.input.type.color).append(Component.text(" -> ").color(
                NamedTextColor.GRAY)).append(Component.text(extraction.output.type.name).color(extraction.output.type.color)).decoration(TextDecoration.ITALIC, false))
            .build()
    }

    fun invForSize(size: Int): InventoryType {
        val rows = (size + 8) / 9
        return when (rows) {
            1 -> InventoryType.CHEST_1_ROW
            2 -> InventoryType.CHEST_2_ROW
            3 -> InventoryType.CHEST_3_ROW
            4 -> InventoryType.CHEST_4_ROW
            5 -> InventoryType.CHEST_5_ROW
            6 -> InventoryType.CHEST_6_ROW
            else -> InventoryType.CHEST_6_ROW
        }
    }

}

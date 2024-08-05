package de.blazemcworld.fireflow.inventory

import de.blazemcworld.fireflow.tool.Tool
import net.minestom.server.entity.Player
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType

object ToolsInventory {
    fun open(player: Player) {
        val inv = Inventory(InventoryType.CHEST_1_ROW, "Tools")

        for ((slot, tool) in Tool.allTools.withIndex()) inv.setItemStack(slot, tool.item)
        player.openInventory(inv)
    }
}
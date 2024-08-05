package de.blazemcworld.fireflow.tool

import de.blazemcworld.fireflow.inventory.SelectionInventories
import de.blazemcworld.fireflow.node.NodeCategory
import de.blazemcworld.fireflow.space.Space
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.Player
import net.minestom.server.item.Material

object CreateNodeTool : Tool {
    override val item = item(Material.SLIME_BLOCK,
        "Create Node", NamedTextColor.GREEN,
        "Used for creating nodes",
        "in the code."
    )

    override fun handler(player: Player, space: Space) = object : Tool.Handler {
        override val tool = CreateNodeTool

        override fun use(callback: (Player, Boolean) -> Unit) {
            SelectionInventories.selectNode(player, space, NodeCategory.root) { node ->
                space.codeNodes += node.newComponent().also {
                    it.pos = space.codeCursor(player)
                    it.update(space.codeInstance)
                }
            }
        }
    }
}
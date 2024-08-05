package de.blazemcworld.fireflow.tool

import de.blazemcworld.fireflow.gui.IOComponent
import de.blazemcworld.fireflow.space.Space
import de.blazemcworld.fireflow.util.sendError
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.Player
import net.minestom.server.item.Material

object InsetLiteralTool : Tool {
    override val item = item(Material.REDSTONE_TORCH,
        "Inset Literal", NamedTextColor.DARK_RED,
        "Used for insetting literals",
        "inside of nodes.",
        "",
        "To use hold and type",
        "value in chat.",
        "",
        "Click on inset val to",
        "remove it."
    )

    override fun handler(player: Player, space: Space) = object : Tool.Handler {
        override val tool = InsetLiteralTool

        var highlighter: Tool.IOHighlighter? = Tool.IOHighlighter(NamedTextColor.DARK_RED, player, space) { it is IOComponent.InsetInput<*> }

        override fun use(callback: (Player, Boolean) -> Unit) {
            val cursor = space.codeCursor(player)
            space.codeNodes.find { it.includes(cursor) }?.let {
                for (input in it.inputs) {
                    if (input.includes(cursor) && input is IOComponent.InsetInput<*>) {
                        input.insetVal = null
                        input.node.update(space.codeInstance)
                        return
                    }
                }
            }
        }

        override fun chat(message: String): Boolean {
            val cursor = space.codeCursor(player)
            var found = false
            space.codeNodes.find { it.includes(cursor) }?.let {
                for (input in it.inputs) {
                    if (input.includes(cursor)) {
                        found = true
                        if (input is IOComponent.InsetInput<*>) {
                            val literal = input.io.type.parse(message, space)
                            if (literal == null) {
                                player.sendError("Value seems invalid!")
                                return@let true
                            }
                            input.updateInset(message, space)
                            input.node.update(space.codeInstance)

                            input.disconnectAll()

                            return@let true
                        } else {
                            player.sendError("This input type does not support inset literals.")
                            return@let true
                        }
                    }
                }
            }

            if (!found) {
                player.sendError("You must be selecting an input to inset a literal!")
            }
            return true
        }

        override fun select() {
            highlighter?.selected()
        }


        override fun deselect() {
            highlighter?.deselect()
        }
    }
}
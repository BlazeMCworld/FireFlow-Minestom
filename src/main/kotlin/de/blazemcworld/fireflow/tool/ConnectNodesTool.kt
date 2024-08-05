package de.blazemcworld.fireflow.tool

import de.blazemcworld.fireflow.gui.ExtractedNodeComponent
import de.blazemcworld.fireflow.gui.IOComponent
import de.blazemcworld.fireflow.gui.LineComponent
import de.blazemcworld.fireflow.gui.Pos2d
import de.blazemcworld.fireflow.inventory.ExtractionInventory
import de.blazemcworld.fireflow.space.Space
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.item.Material
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule

object ConnectNodesTool : Tool {
    override val item = item(Material.BREEZE_ROD,
        "Connect Nodes", NamedTextColor.AQUA,
        "Used for connecting node",
        "inputs and outputs.",
        "",
        "Press 'F' to extract a",
        "value from an output."
    )

    override fun handler(player: Player, space: Space) = object : Tool.Handler {
        override val tool = ConnectNodesTool

        private var from: IOComponent.Output? = null
        private val relays = mutableListOf<Pos2d>()
        private var otherLines = mutableListOf<LineComponent>()
        private var previewLine = LineComponent()
        private var previewTask: Task? = null

        private var highlighter: Tool.IOHighlighter? = Tool.IOHighlighter(NamedTextColor.AQUA, player, space)

        override fun use(callback: (Player, Boolean) -> Unit) {
            val cursor = space.codeCursor(player)
            for (node in space.codeNodes) {
                for (output in node.outputs) {
                    if (output.includes(cursor)) {
                        if (from == output) {
                            clearSelectionPreview()
                            return
                        }
                        clearSelectionPreview()
                        from = output
                        previewLine = LineComponent()
                        previewLine.color = output.io.type.color

                        previewTask?.cancel()
                        previewTask = MinecraftServer.getSchedulerManager().submitTask {
                            if (otherLines.isEmpty()) {
                                previewLine.start = Pos2d(output.pos.x, output.pos.y + output.text.height() * 0.75)
                            } else {
                                otherLines[0].start = Pos2d(output.pos.x, output.pos.y + output.text.height() * 0.75)
                            }
                            previewLine.end = space.codeCursor(player)
                            previewLine.update(space.codeInstance)
                            return@submitTask TaskSchedule.tick(1)
                        }
                        return
                    }
                }
                if (from == null) for (input in node.inputs) {
                    for (conn in input.connections) {
                        for ((index, pos) in conn.relays.withIndex()) {
                            if (pos.distance(cursor) < 0.2) {
                                from = conn.output
                                previewLine = LineComponent()
                                previewLine.color = conn.output.io.type.color

                                for (each in 0..index) {
                                    relays += conn.relays[each]
                                    previewLine.end = conn.relays[each]
                                    otherLines.add(previewLine)
                                    previewLine = LineComponent()
                                    previewLine.start = conn.relays[each]
                                    previewLine.color = conn.output.io.type.color
                                }

                                previewTask?.cancel()
                                previewTask = MinecraftServer.getSchedulerManager().submitTask {
                                    if (otherLines.isEmpty()) {
                                        previewLine.start = Pos2d(conn.output.pos.x, conn.output.pos.y + conn.output.text.height() * 0.75)
                                    } else {
                                        otherLines[0].start = Pos2d(conn.output.pos.x, conn.output.pos.y + conn.output.text.height() * 0.75)
                                    }
                                    previewLine.end = space.codeCursor(player)
                                    previewLine.update(space.codeInstance)
                                    return@submitTask TaskSchedule.tick(1)
                                }
                                return
                            }
                        }
                    }
                }
                from?.let { output ->
                    for (input in node.inputs) {
                        if (input.includes(cursor)) {
                            if (!input.connect(output, relays)) return

                            if (input is IOComponent.InsetInput<*> && input.insetVal != null) {
                                input.insetVal = null
                            }

                            input.node.update(space.codeInstance)
                            clearSelectionPreview()
                            return
                        }
                    }
                }
            }
            from?.let {
                relays.add(cursor)
                previewLine.end = cursor
                otherLines.add(previewLine)
                previewLine = LineComponent()
                previewLine.start = cursor
                previewLine.color = it.io.type.color
            }
        }

        override fun swap(callback: (Player, Boolean) -> Unit): Boolean {
            // Open Extraction Menu
            ExtractionInventory.openForType(player, from?.io?.type ?: return true) {
                space.codeNodes += ExtractedNodeComponent(it).also { node ->
                    node.pos = space.codeCursor(player)
                    node.update(space.codeInstance)

                    val input = node.inputs[0]
                    if (from?.connect(input, relays) == true) {
                        if (input is IOComponent.InsetInput<*> && input.insetVal != null) {
                            input.insetVal = null
                        }

                        callback(player, true)
                        input.node.update(space.codeInstance)
                        clearSelectionPreview()
                    }
                }
            }

            return true
        }

        override fun select() {
            highlighter?.selected()
        }

        override fun hasSelection() = from != null

        fun clearSelectionPreview() {
            from = null
            previewLine.remove()
            previewTask?.cancel()
            previewTask = null
            for (other in otherLines) other.remove()
            otherLines.clear()
            relays.clear()
        }

        override fun deselect() {
            clearSelectionPreview()
            highlighter?.deselect()
        }
    }
}
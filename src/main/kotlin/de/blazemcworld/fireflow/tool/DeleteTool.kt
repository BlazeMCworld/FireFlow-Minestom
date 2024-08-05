package de.blazemcworld.fireflow.tool

import de.blazemcworld.fireflow.database.DatabaseHelper
import de.blazemcworld.fireflow.gui.ConnectionComponent
import de.blazemcworld.fireflow.gui.NodeComponent
import de.blazemcworld.fireflow.gui.Pos2d
import de.blazemcworld.fireflow.gui.RectangleComponent
import de.blazemcworld.fireflow.inventory.DeleteInventory
import de.blazemcworld.fireflow.node.FunctionCallNode
import de.blazemcworld.fireflow.node.FunctionInputsNode
import de.blazemcworld.fireflow.node.FunctionOutputsNode
import de.blazemcworld.fireflow.space.Space
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.item.Material
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule
import kotlin.math.max
import kotlin.math.min

object DeleteTool : Tool {
    override val item = item(Material.REDSTONE_BLOCK,
        "Delete", NamedTextColor.RED,
        "Used for deleting nodes",
        "or connections in the code."
    )

    override fun handler(player: Player, space: Space) = object : Tool.Handler {
        override val tool = DeleteTool

        val nodes = mutableMapOf<NodeComponent, Pos2d>()
        var connection: ConnectionComponent? = null
        val connections = mutableMapOf<ConnectionComponent, List<Pos2d>>()
        var selectionStart: Pos2d? = null
        var selectionIndicator = RectangleComponent()
        var selectionTask: Task? = null

        override fun use(callback: (Player, Boolean) -> Unit) {
            val cursor = space.codeCursor(player)
            selectionStart?.let { start ->
                selectionStart = null
                selectionTask?.cancel()
                selectionIndicator.remove()

                val min = Pos2d(min(start.x, cursor.x), min(start.y, cursor.y))
                val max = Pos2d(max(start.x, cursor.x), max(start.y, cursor.y))

                space.codeNodes.forEach {
                    if (it.isBeingMoved) return@forEach
                    if (it.outline.pos.x < min.x || it.outline.pos.x + it.outline.size.x > max.x) return@forEach
                    if (it.outline.pos.y < min.y || it.outline.pos.y + it.outline.size.y > max.y) return@forEach

                    nodes[it] = it.pos - cursor
                    it.outline.setColor(NamedTextColor.RED)
                    it.isBeingMoved = true
                    it.update(space.codeInstance)
                }

                fun deleteNodes(delete: Boolean) {
                    if (delete) {
                        for ((node, _) in nodes) {
                            node.remove()
                            space.codeNodes.remove(node)
                        }
                        nodes.clear()
                    }
                    callback(player, true)
                    clearSelection()
                }

                if (nodes.size >= 5 && DatabaseHelper.getPreference(player, "delete-warning") == 0.toByte()) {
                    DeleteInventory.open(player, ::deleteNodes)
                } else {
                    deleteNodes(true)
                }
                return
            }
            callback(player, true)
            space.codeNodes.find { it.includes(cursor) }?.let {
                if (it.isBeingMoved) return
                if (it.node is FunctionInputsNode) {
                    if (space.codeNodes.any { call -> call.node is FunctionCallNode && call.node.fn == it.node.fn }) return
                }
                if (it.node is FunctionOutputsNode) {
                    if (space.codeNodes.any { call -> call.node is FunctionCallNode && call.node.fn == it.node.fn }) return
                }
                it.remove()
                space.codeNodes.remove(it)
                if (it.node is FunctionInputsNode) {
                    space.functions.removeIf { other ->
                        other.first.fn == it.node.fn
                    }
                    space.functionNodes.removeIf { f -> f.fn == it.node.fn }
                    space.codeNodes.removeIf { other ->
                        if (other.node !is FunctionOutputsNode || other.node.fn != it.node.fn) return@removeIf false
                        other.remove()
                        return@removeIf true
                    }
                }
                if (it.node is FunctionOutputsNode) {
                    space.functions.removeIf { other ->
                        other.first.fn == it.node.fn
                    }
                    space.functionNodes.removeIf { f -> f.fn == it.node.fn }
                    space.codeNodes.removeIf { other ->
                        if (other.node !is FunctionInputsNode || other.node.fn != it.node.fn) return@removeIf false
                        other.remove()
                        return@removeIf true
                    }
                }
                return@let
            }

            for (node in space.codeNodes) {
                for (input in node.inputs) {
                    for (line in input.connections) {
                        if (line.distance(cursor) < 0.1) {
                            for ((index, pos) in line.relays.withIndex()) {
                                if (pos.distance(cursor) < 0.2) {
                                    line.relays.removeAt(index)
                                    line.update(space.codeInstance)
                                    return
                                }
                            }
                            input.connections.remove(line)
                            line.output.connections.remove(input)
                            input.update(space.codeInstance)
                            line.remove()
                            return
                        }
                    }
                }
            }

            if (nodes.isEmpty() && connection == null) {
                selectionStart = cursor
                selectionTask?.cancel()
                selectionIndicator.remove()
                selectionIndicator = RectangleComponent()
                selectionIndicator.setColor(NamedTextColor.RED)
                selectionIndicator.pos = cursor
                selectionTask = MinecraftServer.getSchedulerManager().submitTask task@{
                    selectionIndicator.size = space.codeCursor(player) - selectionIndicator.pos
                    selectionIndicator.update(space.codeInstance)
                    return@task TaskSchedule.tick(1)
                }
                return
            }
        }

        override fun hasSelection() = nodes.isNotEmpty()

        override fun startedSelection() = selectionStart != null

        private fun clearSelection() {
            nodes.forEach { (node, _) ->
                node.restoreBorder()
                node.isBeingMoved = false
                node.update(space.codeInstance)
            }
            nodes.clear()
            connections.clear()
            connection = null
            selectionTask?.cancel()
            selectionIndicator.remove()
        }

        override fun deselect() {
            clearSelection()
        }
    }
}
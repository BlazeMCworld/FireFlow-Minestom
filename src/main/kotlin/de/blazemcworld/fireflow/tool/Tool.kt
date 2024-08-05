package de.blazemcworld.fireflow.tool

import de.blazemcworld.fireflow.gui.IOComponent
import de.blazemcworld.fireflow.gui.Pos2d
import de.blazemcworld.fireflow.gui.RectangleComponent
import de.blazemcworld.fireflow.space.Space
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule

interface Tool {

    val item: ItemStack
    fun handler(player: Player, space: Space): Handler

    companion object {
        val allTools = setOf(
            CreateNodeTool,
            ConnectNodesTool,
            MoveTool,
            DeleteTool,
            InsetLiteralTool
        )
    }

   interface Handler {
       val tool: Tool
       fun select() {}
       fun deselect() {}
       fun use(callback: (Player, Boolean) -> Unit = { _, _ ->}) {}
       fun swap(callback: (Player, Boolean) -> Unit = { _, _ ->}): Boolean { return false}
       fun chat(message: String): Boolean { return false }
       fun hasSelection() = false
       fun startedSelection() = false
   }

    class IOHighlighter(val color: TextColor, val player: Player, val space: Space, val callback: (Player, IOComponent?) -> Unit = {_, _ ->}, val filter: (IOComponent) -> Boolean = {true}) {
        private val box = RectangleComponent().apply {
            setColor(color)
        }
        private var task: Task? = null
        private var selected: IOComponent? = null

        fun selected() {
            task = MinecraftServer.getSchedulerManager().submitTask {
                val cursor = space.codeCursor(player)
                space.codeNodes.find { it.includes(cursor) }?.let {
                    var found = false
                    for (io in it.inputs + it.outputs) {
                        if (io.includes(cursor) && filter(io)) {
                            box.pos = io.text.pos.plus(Pos2d(-.12, .05))
                            box.size = io.text.size().plus(Pos2d(.2, 0))
                            box.update(space.codeInstance)
                            callback(player, io)
                            selected = io
                            found = true
                            break
                        }
                    }
                    if (!found) {
                        selected = null
                        callback(player, null)
                        box.pos = cursor
                        box.size = Pos2d(0.1, 0.1)
                        box.update(space.codeInstance)
                    }
                    return@submitTask TaskSchedule.tick(1)
                }
                callback(player, null)
                selected = null

                box.pos = cursor
                box.size = Pos2d(0.1, 0.1)
                box.update(space.codeInstance)

                return@submitTask TaskSchedule.tick(1)
            }
        }

        fun setColor(color: TextColor) = box.setColor(color)

        fun hasSelection() = selected != null

        fun getSelected() = selected

        fun deselect() {
            task?.cancel()
            box.remove()
        }
    }

    fun item(material: Material, name: String, color: TextColor, vararg description: String) = ItemStack.builder(material)
        .customName(Component.text(name).color(color).decoration(TextDecoration.ITALIC, false))
        .lore(description.map {
            Component.text(it).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        }).build()
}
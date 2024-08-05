package de.blazemcworld.fireflow.gui

import de.blazemcworld.fireflow.node.BaseNode
import de.blazemcworld.fireflow.node.SignalType
import de.blazemcworld.fireflow.node.ValueType
import de.blazemcworld.fireflow.space.Space
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.instance.Instance
import kotlin.math.max
import kotlin.math.min

abstract class IOComponent(val node: NodeComponent) {

    val text = TextComponent()
    var pos = Pos2d.ZERO

    open fun update(inst: Instance) {
        text.pos = pos
        text.update(inst)
    }

    fun remove() {
        text.remove()
        disconnectAll()
    }

    fun includes(cursor: Pos2d) = text.includes(cursor)

    abstract fun disconnectAll()

    open class Input(val io: BaseNode.Input<*>, node: NodeComponent) : IOComponent(node) {
        val connections = mutableSetOf<ConnectionComponent>()

        init {
            updateText()
        }

        private fun updateText() {
            if (this is InsetInput<*> && insetVal != null) {
                val display = stringify()

                text.text = Component.text("⏹ " + display.substring(0..max(0,min(display.length-1, 10))) + (if (display.length > 10) "..." else "") ).color(io.type.color)
            } else {
                text.text = Component.text("○ " + io.name).color(io.type.color)
                if (io.optional) text.text = text.text.append(Component.text("*").color(NamedTextColor.GRAY))
            }
        }

        fun connect(output: Output, relays: List<Pos2d>): Boolean {
            if (output.io.type != io.type) return false

            if (io.type is SignalType) {
                output.disconnectAll()
            } else {
                disconnectAll()
            }
            connections.add(ConnectionComponent(this, output).also { it.relays += relays })
            output.connections.add(this)
            return true
        }

        override fun disconnectAll() {
            for (connection in connections) {
                connection.output.connections.remove(this)
            }
            for (line in connections) {
                line.remove()
            }
            connections.clear()
        }

        override fun update(inst: Instance) {
            updateText()

            for (connection in connections) {
                connection.update(inst)
            }
            super.update(inst)
        }
    }

    class InsetInput<T : Any>(val input : BaseNode.Input<T>, node: NodeComponent, var insetVal: T? = input.default, val type: ValueType<T> = input.type) : Input(input, node) {
        fun stringify() = insetVal?.let { type.stringify(it) } ?: "unset"

        fun updateInset(string: String, space: Space) {
            insetVal = type.parse(string, space)
        }
    }

    class Output(val io: BaseNode.Output<*>, node: NodeComponent) : IOComponent(node) {
        val connections = mutableSetOf<Input>()
        init {
            text.text = Component.text(io.name + " ○").color(io.type.color)
        }

        fun connect(input: Input, relays: List<Pos2d>) = input.connect(this, relays)

        override fun disconnectAll() {
            for (input in connections) input.connections.removeIf { (it.output != this).apply { if (this) it.remove() } }
            connections.clear()
        }

        override fun update(inst: Instance) {
            for (input in connections) input.update(inst)
            super.update(inst)
        }
    }
}
package de.blazemcworld.fireflow.node

import de.blazemcworld.fireflow.gui.IOComponent
import de.blazemcworld.fireflow.gui.NodeComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

abstract class Node(val title: String, val material: Material) {
    abstract fun menuItem(): ItemStack
}

abstract class GenericNode(title: String, material: Material) : Node(title, material) {
    abstract fun create(generics: Map<String, ValueType<*>>): BaseNode

    val inputs = mutableListOf<IO>()
    private val outputs = mutableListOf<IO>()
    val generics = mutableMapOf<String, List<SomeType>>()

    override fun menuItem() = ItemStack.builder(material)
        .customName(Component.text(title).color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)).also {
            val lore = mutableListOf<Component>()
            if (inputs.isNotEmpty()) {
                lore.add(Component.text("Needs:").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                for (input in inputs) {
                    val color = if (input is TypedIO<*>) input.type.color else NamedTextColor.WHITE
                    lore.add(Component.text("- ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(input.name).color(color)))
                }
            }
            if (outputs.isNotEmpty()) {
                lore.add(Component.text("Provides:").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                for (output in outputs) {
                    val color = if (output is TypedIO<*>) output.type.color else NamedTextColor.WHITE
                    lore.add(Component.text("- ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(output.name).color(color)))
                }
            }
            it.lore(lore)
        }
        .hideExtraTooltip()
        .build()

    fun <T : Any> input(name: String, type: ValueType<T>) {
        inputs += TypedIO(name, type)
    }

    fun <T : Any> output(name: String, type: ValueType<T>) {
        outputs += TypedIO(name, type)
    }

    fun generic(id: String, options: List<SomeType>) {
        generics[id] = options
    }

    fun genericInput(name: String) {
        inputs += GenericIO(name)
    }

    fun genericOutput(name: String) {
        outputs += GenericIO(name)
    }

    open class IO(val name: String)
    class GenericIO(name: String) : IO(name)
    class TypedIO<T : Any>(name: String, val type: ValueType<T>): IO(name)
}

abstract class BaseNode(title: String, material: Material) : Node(title, material) {
    val inputs = mutableListOf<Input<*>>()
    val outputs = mutableListOf<Output<*>>()
    open val generics = emptyMap<String, ValueType<*>>()
    open val generic: GenericNode? = null

    companion object {
        val VOID = object : BaseNode("Void", Material.BARRIER) {
            override fun setup(ctx: NodeContext) {}
        }
    }

    fun <T : Any> input(name: String, type: ValueType<T>, default: T? = null, optional: Boolean = false): Input<T> {
        if (type.insetable) return Input(name, type, default, optional).also { inputs += it }
        return Input(name, type, optional=optional).also { inputs += it }
    }

    fun <T : Any> output(name: String, type: ValueType<T>) = Output(name, type).also { outputs += it }

    override fun menuItem() = ItemStack.builder(material)
        .customName(Component.text(title).color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)).also {
            val lore = mutableListOf<Component>()
            if (inputs.isNotEmpty()) {
                lore.add(Component.text("Needs:").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                for (input in inputs) {
                    val loreLine = Component.text("- ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(input.name).color(input.type.color))
                    if (input.optional) loreLine.append(Component.text("*").color(NamedTextColor.GRAY))
                    lore.add(loreLine)
                }
            }
            if (outputs.isNotEmpty()) {
                lore.add(Component.text("Provides:").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                for (output in outputs) {
                    lore.add(Component.text("- ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(output.name).color(output.type.color)))
                }
            }
            it.lore(lore)
        }
        .hideExtraTooltip()
        .build()

    abstract fun setup(ctx: NodeContext)

    fun newComponent(): NodeComponent {
        val c = NodeComponent(this)
        c.title.text = Component.text(title)
        for (i in inputs) {
            if (i.type.insetable) c.inputs.add(IOComponent.InsetInput(i, c))
            else c.inputs.add(IOComponent.Input(i, c))
        }
        for (o in outputs) c.outputs.add(IOComponent.Output(o, c))
        return c
    }

    interface IO<T : Any> {
        val name: String
        val type: ValueType<T>
    }
    open class Input<T : Any>(override val name: String, override val type: ValueType<T>, val default: T? = null, val optional: Boolean = false): IO<T>
    open class Output<T : Any>(override val name: String, override val type: ValueType<T>): IO<T>
}
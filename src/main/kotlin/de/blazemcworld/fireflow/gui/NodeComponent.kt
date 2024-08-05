@file:Suppress("UnstableApiUsage")

package de.blazemcworld.fireflow.gui

import de.blazemcworld.fireflow.node.*
import de.blazemcworld.fireflow.node.impl.ValueLiteralNode
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.instance.Instance
import net.minestom.server.network.NetworkBuffer
import kotlin.math.max
import kotlin.math.min

private const val PADDING = 0.1
private const val DOUBLE_PADDING = PADDING * 2
private const val CENTER_SPACING = 0.2

class ExtractedNodeComponent(val extraction: TypeExtraction<*, *>) : NodeComponent(BaseNode.VOID) {
    override val type = NodeComponentType.EXTRACTION
    override val renderTitle = false
    private val borderColor = extraction.input.type.color

    init {
        inputs += IOComponent.Input(extraction.input, this)
        outputs += IOComponent.Output(extraction.output, this)

        title.text = Component.text("")
        outline.setColor(borderColor)
    }

    override fun restoreBorder() {
        outline.setColor(borderColor)
    }

}

open class NodeComponent(val node: BaseNode) {
    open val type = NodeComponentType.BASE
    var valueLiteral: String? = null
    private val valueDisplay = TextComponent()
    var pos = Pos2d.ZERO
    var isBeingMoved = false
    val title = TextComponent()
    open val renderTitle = true
    val inputs = mutableListOf<IOComponent.Input>()
    val outputs = mutableListOf<IOComponent.Output>()
    val outline = RectangleComponent()

    fun update(inst: Instance) {
        val baseY = pos.y
        var inputY = 0.0
        var outputY = 0.0
        var inputWidth = 0.0
        var outputWidth = 0.0

        if (node is ValueLiteralNode<*>) inputWidth = TextWidth.calculate(valueLiteral ?: DEFAULT_LITERAL) / 40

        for (i in inputs) {
            i.update(inst)
            inputWidth = max(inputWidth, i.text.width())
        }

        for (o in outputs) {
            outputWidth = max(outputWidth, o.text.width())
        }
        if (inputWidth + outputWidth < title.width()) {
            val diff = (title.width() - inputWidth - outputWidth) * 0.5
            inputWidth += diff
            outputWidth += diff
        }
        inputWidth += CENTER_SPACING
        if (node is ValueLiteralNode<*>) {
            valueDisplay.text = Component.text(valueLiteral ?: DEFAULT_LITERAL).color(node.type.color)
            valueDisplay.pos = Pos2d(pos.x + inputWidth - valueDisplay.width(), inputY + baseY)
            inputY -= valueDisplay.height()
            valueDisplay.update(inst)
        }

        for (o in outputs) {
            o.pos = Pos2d(pos.x - outputWidth, outputY + baseY)
            outputY -= o.text.height()
            o.update(inst)
        }

        for (i in inputs) {
            i.pos = Pos2d(pos.x + inputWidth - i.text.width(), inputY + baseY)
            inputY -= i.text.height()
            i.update(inst)
        }

        if (renderTitle) {
            title.pos = Pos2d(pos.x - title.width() * 0.5 + (inputWidth - outputWidth) * 0.5, pos.y + title.height())
            title.update(inst)
            outline.pos = Pos2d(pos.x - outputWidth - PADDING, pos.y + min(inputY, outputY) + title.height() - PADDING)
            outline.size = Pos2d(inputWidth + outputWidth + DOUBLE_PADDING, -min(inputY, outputY) + title.height() + DOUBLE_PADDING)
        } else {
            outline.pos = Pos2d(pos.x - outputWidth - PADDING, pos.y + inputY + DOUBLE_PADDING)
            outline.size = Pos2d(inputWidth + outputWidth + DOUBLE_PADDING, -min(inputY, outputY) + DOUBLE_PADDING)
        }

        outline.update(inst)
        postUpdate(inst)
    }

    open fun postUpdate(inst: Instance) {}

    fun remove() {
        title.remove()
        outline.remove()
        valueDisplay.remove()
        inputs.forEach(IOComponent::remove)
        outputs.forEach(IOComponent::remove)
    }

    open fun restoreBorder() {
        outline.setColor(NamedTextColor.WHITE)
    }

    fun includes(pos: Pos2d) = outline.includes(pos)

    companion object {
        const val DEFAULT_LITERAL = "unset"
    }
}

enum class NodeComponentType(val read: NetworkBuffer.(Set<Node>) -> NodeComponent?, val write: NetworkBuffer.(NodeComponent) -> Unit) {
    BASE({ search ->
        read(NetworkBuffer.STRING).let { id -> (search.find { it is BaseNode && it.title == id } as? BaseNode)?.newComponent() }
    }, { write(NetworkBuffer.STRING, it.node.title) }),
    GENERIC(n@{ search ->
        val id = read(NetworkBuffer.STRING)
        val type = search.find { it is GenericNode && it.title == id } as? GenericNode ?: return@n null
        val genericsSize = read(NetworkBuffer.VAR_INT)
        type.create(buildMap(genericsSize) {
            repeat(genericsSize) { this[read(NetworkBuffer.STRING)] = readType(this@n) ?: return@n null }
        }).newComponent()
    }, { it.node.generic?.let { genericType ->
        write(NetworkBuffer.STRING, genericType.title)
        write(NetworkBuffer.VAR_INT, it.node.generics.size)
        for ((k, v) in it.node.generics) {
            write(NetworkBuffer.STRING, k) // name
            writeType(this, v) // type
        }
    } }),
    EXTRACTION({ TypeExtraction.list[read(NetworkBuffer.STRING)]?.let { ExtractedNodeComponent(it) } }, {
        if (it is ExtractedNodeComponent) write(NetworkBuffer.STRING, it.extraction.formalName)
    }),
    ;
}
package de.blazemcworld.fireflow.node.impl

import de.blazemcworld.fireflow.node.*
import net.minestom.server.item.Material
import java.util.*

object ConcatNode : BaseNode("Concatenate", Material.TRIPWIRE_HOOK) {
    private val left = input("Left", TextType)
    private val right = input("Right", TextType)

    private val result = output("Result", TextType)

    override fun setup(ctx: NodeContext) {
        ctx[result].defaultHandler = { it[ctx[left]] + it[ctx[right]] }
    }
}

object SubtextNode : BaseNode("Subtext", Material.SHEARS) {
    private val text = input("Text", TextType)
    private val min = input("Min", NumberType, 0.0)
    private val max = input("Max", NumberType)

    private val result = output("Result", TextType)

    override fun setup(ctx: NodeContext) {
        ctx[result].defaultHandler = {
            val text = it[ctx[text]]
            val length = text?.length ?: 0
            var min = it[ctx[min]]?.toInt() ?: 0
            var max = it[ctx[max]]?.toInt() ?: length

            if (min < 0) min = 0 else if (min >= length) min = length
            if (max < 0) max = 0 else if (max >= length) max = length
            if (min < max) min = max.also { max = min }

            text?.substring(min, max) ?: ""
        }
    }
}

object ToTextNode : GenericNode("To Text", Material.STRING) {
    private val cache = WeakHashMap<ValueType<*>, Impl<*>>()
    override fun create(generics: Map<String, ValueType<*>>): Impl<*> = cache.computeIfAbsent(generics["Type"]) { Impl(generics["Type"]!!) }

    init {
        generic("Type", AllTypes.dataOnly)
        genericInput("Input")
        output("Output", TextType)
    }

    class Impl<T : Any>(val type: ValueType<T>) : BaseNode("To Text", type.material) {
        private val input = input("Input", type)
        private val output = output("Output", TextType)
        override val generics = mapOf("Type" to type)
        override val generic = ToTextNode

        override fun setup(ctx: NodeContext) {
            ctx[output].defaultHandler = {
                val input = it[ctx[input]]
                if (input == null) ""
                else type.stringify(input)
            }
        }
    }
}

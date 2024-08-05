package de.blazemcworld.fireflow.node.impl

import de.blazemcworld.fireflow.node.*
import net.minestom.server.item.Material
import java.util.*

object IfNode : BaseNode("If Condition", Material.IRON_INGOT) {
    private val signal = input("Signal", SignalType)
    private val condition = input("Condition", ConditionType)

    private val whenTrue = output("True", SignalType)
    private val whenFalse = output("False", SignalType)

    override fun setup(ctx: NodeContext) {
        ctx[signal].signalListener = {
            if (it[ctx[condition]] == true) {
                it.emit(ctx[whenTrue])
            } else {
                it.emit(ctx[whenFalse])
            }
        }
    }
}

object GreaterThanNode : BaseNode("Greater Than", Material.COMPARATOR) {
    private val left = input("Left", NumberType, optional=true)
    private val right = input("Right", NumberType, optional=true)
    private val condition = output("Condition", ConditionType)

    override fun setup(ctx: NodeContext) {
        ctx[condition].defaultHandler = {
            (it[ctx[left]] ?: 0.0) > (it[ctx[right]] ?: 0.0)
        }
    }
}

object EqualNode : GenericNode("Equal", Material.RAW_IRON) {
    private val cache = WeakHashMap<ValueType<*>, Impl<*>>()
    override fun create(generics: Map<String, ValueType<*>>): Impl<*> = cache.computeIfAbsent(generics["Type"]) { Impl(generics["Type"]!!) }

    init {
        generic("Type", AllTypes.dataOnly)
        genericInput("Left")
        genericOutput("Right")
        output("Case", ConditionType)
    }

    class Impl<T : Any>(private val type: ValueType<T>) : BaseNode(type.name + " Equal", type.material) {
        private val left = input("Left", type)
        private val right = input("Right", type)
        private val condition = output("Case", ConditionType)
        override val generics = mapOf("Type" to type)
        override val generic = EqualNode

        override fun setup(ctx: NodeContext) {
            ctx[condition].defaultHandler = {
                type.compareEqual(it[ctx[left]], it[ctx[right]])
            }
        }
    }
}
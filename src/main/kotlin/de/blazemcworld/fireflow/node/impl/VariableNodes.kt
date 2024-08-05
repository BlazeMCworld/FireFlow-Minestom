package de.blazemcworld.fireflow.node.impl

import de.blazemcworld.fireflow.node.*
import net.minestom.server.item.Material
import java.util.*

class SetVariableNode(private val store: VariableStore) : GenericNode("Set ${store.type} Variable", Material.IRON_BLOCK) {
    private val cache = WeakHashMap<ValueType<*>, Impl<*>>()
    override fun create(generics: Map<String, ValueType<*>>): Impl<*> = cache.computeIfAbsent(generics["Type"]) { Impl(generics["Type"]!!, store, this) }

    init {
        generic("Type", AllTypes.dataOnly)
        input("Signal", SignalType)
        input("Name", TextType)
        genericInput("Value")
        output("Next", SignalType)
    }

    class Impl<T : Any>(val type: ValueType<T>, private val store: VariableStore, override val generic: SetVariableNode) : BaseNode("Set ${store.type} ${type.name} Variable", type.material) {
        private val signal = input("Signal", SignalType)
        private val name = input("Name", TextType)
        private val value = input("Value", type)
        private val next = output("Next", SignalType)
        override val generics = mapOf("Type" to type)

        override fun setup(ctx: NodeContext) {
            ctx[signal].signalListener = {
                it[ctx[name]]?.let { n ->
                    store.setVariable(it, n, it[ctx[value]], type)
                }
                it.emit(ctx[next])
            }
        }
    }
}

class GetVariableNode(private val store: VariableStore) : GenericNode("Get ${store.type} Variable", Material.IRON_NUGGET) {
    private val cache = WeakHashMap<ValueType<*>, Impl<*>>()
    override fun create(generics: Map<String, ValueType<*>>): Impl<*> = cache.computeIfAbsent(generics["Type"]) { Impl(generics["Type"]!!, store, this) }

    init {
        generic("Type", AllTypes.dataOnly)
        input("Name", TextType)
        genericOutput("Value")
    }

    class Impl<T : Any>(val type: ValueType<T>, private val store: VariableStore, override val generic: GetVariableNode) : BaseNode("Get ${store.type} ${type.name} Variable", type.material) {
        private val name = input("Name", TextType)
        private val value = output("Value", type)
        override val generics = mapOf("Type" to type)

        override fun setup(ctx: NodeContext) {
            ctx[value].defaultHandler = {
                var out: T? = null
                it[ctx[name]]?.let { n ->
                    out = type.validate(store.getVariable(it, n))
                }
                out
            }
        }
    }
}

object VariableNodes {
    val getLocal = GetVariableNode(VariableStore.Local)
    val setLocal = SetVariableNode(VariableStore.Local)
    val getSpace = GetVariableNode(VariableStore.Space)
    val setSpace = SetVariableNode(VariableStore.Space)
    val getPersistent = GetVariableNode(VariableStore.Persistent)
    val setPersistent = SetVariableNode(VariableStore.Persistent)

    val all = listOf(getLocal, setLocal, getSpace, setSpace, getPersistent, setPersistent)
}

interface VariableStore {
    val type: String
    fun getVariable(ctx: EvaluationContext, name: String): Any?
    fun setVariable(ctx: EvaluationContext, name: String, value: Any?, type: ValueType<*>)

    object Local : VariableStore {
        override val type = "Local"

        override fun getVariable(ctx: EvaluationContext, name: String): Any? = ctx.varStore[name]

        override fun setVariable(ctx: EvaluationContext, name: String, value: Any?, type: ValueType<*>) {
            value?.let { ctx.varStore[name] = it } ?: ctx.varStore.remove(name)
        }
    }

    object Space : VariableStore {
        override val type = "Space"

        override fun getVariable(ctx: EvaluationContext, name: String): Any? = ctx.global.varStore[name]

        override fun setVariable(ctx: EvaluationContext, name: String, value: Any?, type: ValueType<*>) {
            value?.let { ctx.global.varStore[name] = it } ?: ctx.global.varStore.remove(name)
        }
    }

    object Persistent : VariableStore {
        override val type = "Persistent"

        override fun getVariable(ctx: EvaluationContext, name: String): Any? {
            return ctx.global.space.varStore[name]
        }

        override fun setVariable(ctx: EvaluationContext, name: String, value: Any?, type: ValueType<*>) {
            value?.let { ctx.global.space.varStore[name] = type to it } ?: ctx.global.space.varStore.remove(name)
        }
    }
}
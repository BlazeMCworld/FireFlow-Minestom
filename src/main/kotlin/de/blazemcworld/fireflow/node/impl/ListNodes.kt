package de.blazemcworld.fireflow.node.impl

import de.blazemcworld.fireflow.node.*
import net.minestom.server.item.Material
import java.util.*

object ListAppendNode : GenericNode("List Append", Material.WEEPING_VINES) {
    init {
        generic("Type", AllTypes.dataOnly)
        input("Signal", SignalType)
        genericInput("List")
        genericInput("Value")
        output("Next", SignalType)
    }

    private val cache = WeakHashMap<ValueType<*>, Impl<*>>()
    override fun create(generics: Map<String, ValueType<*>>): Impl<*> = cache.computeIfAbsent(generics["Type"]) { Impl(generics["Type"]!!) }

    class Impl<T : Any>(type: ValueType<T>) : BaseNode("List(${type.name}) Append", type.material) {
        private val signal = input("Signal", SignalType)
        private val list = input("List", ListType.create(type))
        private val value = input("Value", type)
        private val next = output("Next", SignalType)

        override val generic = ListAppendNode
        override val generics = mapOf("Type" to type)

        override fun setup(ctx: NodeContext) {
            ctx[signal].signalListener = {
                it[ctx[value]]?.let { v -> it[ctx[list]]?.store?.add(v) }
                it.emit(ctx[next])
            }
        }

    }
}

object ListLengthNode : GenericNode("List Length", Material.BLAZE_ROD) {
    init {
        generic("Type", AllTypes.dataOnly)
        genericInput("List")
        output("Length", NumberType)
    }

    private val cache = WeakHashMap<ValueType<*>, Impl<*>>()
    override fun create(generics: Map<String, ValueType<*>>): Impl<*> = cache.computeIfAbsent(generics["Type"]) { Impl(generics["Type"]!!) }

    class Impl<T : Any>(type: ValueType<T>) : BaseNode("List(${type.name}) Length", type.material) {
        private val list = input("List", ListType.create(type))
        private val size = output("Length", NumberType)

        override val generic = ListLengthNode
        override val generics = mapOf("Type" to type)

        override fun setup(ctx: NodeContext) {
            ctx[size].defaultHandler = {
                it[ctx[list]]?.store?.size?.toDouble() ?: 0.0
            }
        }

    }
}

object ListGetNode : GenericNode("List Get", Material.TWISTING_VINES) {
    init {
        generic("Type", AllTypes.dataOnly)
        genericInput("List")
        input("Index", NumberType)
        genericOutput("Value")
    }

    private val cache = WeakHashMap<ValueType<*>, Impl<*>>()
    override fun create(generics: Map<String, ValueType<*>>): Impl<*> = cache.computeIfAbsent(generics["Type"]) { Impl(generics["Type"]!!) }

    class Impl<T : Any>(type: ValueType<T>) : BaseNode("List(${type.name}) Get", type.material) {
        private val list = input("List", ListType.create(type))
        private val index = input("Index", NumberType)
        private val value = output("Value", type)

        override val generic = ListGetNode
        override val generics = mapOf("Type" to type)

        override fun setup(ctx: NodeContext) {
            ctx[value].defaultHandler = v@{
                val store = it[ctx[list]]?.store ?: return@v null
                val index = it[ctx[index]]?.toInt() ?: return@v null
                if (index !in store.indices) return@v null
                store[index]
            }
        }
    }
}


object EmptyListNode : GenericNode("Empty List", Material.STRING) {
    init {
        generic("Type", AllTypes.dataOnly)
        genericOutput("List")
    }

    private val cache = WeakHashMap<ValueType<*>, Impl<*>>()
    override fun create(generics: Map<String, ValueType<*>>): Impl<*> = cache.computeIfAbsent(generics["Type"]) { Impl(generics["Type"]!!) }

    class Impl<T : Any>(val type: ValueType<T>) : BaseNode("Empty List(${type.name})", type.material) {
        private val list = output("List", ListType.create(type))

        override val generic = EmptyListNode
        override val generics = mapOf("Type" to type)

        override fun setup(ctx: NodeContext) {
            ctx[list].defaultHandler = { ListReference(type, mutableListOf()) }
        }
    }
}
object ListRemoveNode : GenericNode("List Remove", Material.TNT) {
    init {
        generic("Type", AllTypes.dataOnly)
        input("Signal", SignalType)
        genericInput("List")
        input("Index", NumberType)
        output("Next", SignalType)
    }

    private val cache = WeakHashMap<ValueType<*>, Impl<*>>()
    override fun create(generics: Map<String, ValueType<*>>): Impl<*> = cache.computeIfAbsent(generics["Type"]) { Impl(generics["Type"]!!) }

    class Impl<T : Any>(type: ValueType<T>) : BaseNode("List(${type.name}) Remove", type.material) {
        private val signal = input("Signal", SignalType)
        private val list = input("List", ListType.create(type))
        private val index = input("Index", NumberType)
        private val next = output("Next", SignalType)

        override val generic = ListRemoveNode
        override val generics = mapOf("Type" to type)

        override fun setup(ctx: NodeContext) {
            ctx[signal].signalListener = {
                it[ctx[index]]?.let { v ->
                    val store = it[ctx[list]]?.store
                    if (store == null || v.toInt() !in store.indices) return@let
                    store.removeAt(v.toInt())
                }
                it.emit(ctx[next])
            }
        }

    }
}

object ListInsertNode : GenericNode("List Insert", Material.SHEARS) {
    init {
        generic("Type", AllTypes.dataOnly)
        input("Signal", SignalType)
        genericInput("List")
        input("Index", NumberType)
        genericInput("Value")
        output("Next", SignalType)
    }

    private val cache = WeakHashMap<ValueType<*>, Impl<*>>()
    override fun create(generics: Map<String, ValueType<*>>): Impl<*> = cache.computeIfAbsent(generics["Type"]) { Impl(generics["Type"]!!) }

    class Impl<T : Any>(type: ValueType<T>) : BaseNode("List(${type.name}) Insert", type.material) {
        private val signal = input("Signal", SignalType)
        private val list = input("List", ListType.create(type))
        private val index = input("Index", NumberType)
        private val value = input("Value", type)
        private val next = output("Next", SignalType)

        override val generic = ListInsertNode
        override val generics = mapOf("Type" to type)

        override fun setup(ctx: NodeContext) {
            ctx[signal].signalListener = {
                it[ctx[value]]?.let { v ->
                    val store = it[ctx[list]]?.store
                    val index = it[ctx[index]] ?: return@let
                    if (store == null || index.toInt() !in store.indices) return@let
                    store.add(index.toInt(), v)
                }
                it.emit(ctx[next])
            }
        }

    }
}

object ListSetNode : GenericNode("List Set", Material.WHITE_WOOL) {
    init {
        generic("Type", AllTypes.dataOnly)
        input("Signal", SignalType)
        genericInput("List")
        input("Index", NumberType)
        genericInput("Value")
        output("Next", SignalType)
    }

    private val cache = WeakHashMap<ValueType<*>, Impl<*>>()
    override fun create(generics: Map<String, ValueType<*>>): Impl<*> = cache.computeIfAbsent(generics["Type"]) { Impl(generics["Type"]!!) }

    class Impl<T : Any>(type: ValueType<T>) : BaseNode("List(${type.name}) Set", type.material) {
        private val signal = input("Signal", SignalType)
        private val list = input("List", ListType.create(type))
        private val index = input("Index", NumberType)
        private val value = input("Value", type)
        private val next = output("Next", SignalType)

        override val generic = ListSetNode
        override val generics = mapOf("Type" to type)

        override fun setup(ctx: NodeContext) {
            ctx[signal].signalListener = {
                it[ctx[value]]?.let { v ->
                    val store = it[ctx[list]]?.store
                    val index = it[ctx[index]] ?: return@let
                    if (store == null || index.toInt() !in store.indices) return@let
                    it[ctx[list]]?.store?.set(index.toInt(), v) }
                it.emit(ctx[next])
            }
        }

    }
}
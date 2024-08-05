package de.blazemcworld.fireflow.node.impl

import de.blazemcworld.fireflow.node.*
import net.minestom.server.item.Material
import java.util.*

object EmptyDictionaryNode : GenericNode("Empty Dictionary", Material.COBWEB) {
    init {
        generic("Key", AllTypes.dataOnly)
        generic("Value", AllTypes.dataOnly)
        genericOutput("Dictionary")
    }

    private val cache = WeakHashMap<Pair<ValueType<*>, ValueType<*>>, Impl<*, *>>()
    override fun create(generics: Map<String, ValueType<*>>): Impl<*, *> = cache.computeIfAbsent(generics["Key"]!! to generics["Value"]!!) { Impl(generics["Key"]!!, generics["Value"]!!) }

    class Impl<K : Any, V : Any>(private val key: ValueType<K>, private val value: ValueType<V>) : BaseNode("Empty Dictionary(${key.name}, ${value.name})", key.material) {
        private val dict = output("Dictionary", DictionaryType.create(key, value))

        override val generic = EmptyDictionaryNode
        override val generics = mapOf("Key" to key, "Value" to value)

        override fun setup(ctx: NodeContext) {
            ctx[dict].defaultHandler = { DictionaryReference(key, value, mutableMapOf()) }
        }
    }
}

object DictionaryGetNode : GenericNode("Dictionary Get", Material.STRING) {
    init {
        generic("Key", AllTypes.dataOnly)
        generic("Value", AllTypes.dataOnly)
        genericInput("Dictionary")
        genericInput("Key")
        genericOutput("Value")
    }

    private val cache = WeakHashMap<Pair<ValueType<*>, ValueType<*>>, Impl<*, *>>()
    override fun create(generics: Map<String, ValueType<*>>): Impl<*, *> = cache.computeIfAbsent(generics["Key"]!! to generics["Value"]!!) { Impl(generics["Key"]!!, generics["Value"]!!) }

    class Impl<K : Any, V : Any>(key: ValueType<K>, value: ValueType<V>) : BaseNode("Dictionary(${key.name}, ${value.name}) Get", key.material) {
        private val dict = input("Dictionary", DictionaryType.create(key, value))
        private val keyIn = input("Key", key)
        private val valueOut = output("Value", value)

        override val generic = DictionaryGetNode
        override val generics = mapOf("Key" to key, "Value" to value)

        override fun setup(ctx: NodeContext) {
            ctx[valueOut].defaultHandler = { it[ctx[dict]]?.store?.get(it[ctx[keyIn]]) }
        }
    }
}

object DictionarySetNode : GenericNode("Dictionary Set", Material.BLUE_WOOL) {
    init {
        generic("Key", AllTypes.dataOnly)
        generic("Value", AllTypes.dataOnly)
        input("Signal", SignalType)
        genericInput("Dictionary")
        genericInput("Key")
        genericInput("Value")
        output("Next", SignalType)
    }

    private val cache = WeakHashMap<Pair<ValueType<*>, ValueType<*>>, Impl<*, *>>()
    override fun create(generics: Map<String, ValueType<*>>): Impl<*, *> = cache.computeIfAbsent(generics["Key"]!! to generics["Value"]!!) { Impl(generics["Key"]!!, generics["Value"]!!) }

    class Impl<K : Any, V : Any>(key: ValueType<K>, value: ValueType<V>) : BaseNode("Dictionary(${key.name}, ${value.name}) Set", key.material) {
        private val signal = input("Signal", SignalType)
        private val dict = input("Dictionary", DictionaryType.create(key, value))
        private val keyIn = input("Key", key)
        private val valueIn = input("Value", value)
        private val next = output("Next", SignalType)

        override val generic = DictionarySetNode
        override val generics = mapOf("Key" to key, "Value" to value)

        override fun setup(ctx: NodeContext) {
            ctx[signal].signalListener = {
                it[ctx[keyIn]]?.let { key ->
                    it[ctx[dict]]?.store?.set(key, it[ctx[valueIn]] ?: return@let)
                }
                it.emit(ctx[next])
            }
        }
    }
}

object DictionaryRemoveNode : GenericNode("Dictionary Remove", Material.RED_WOOL) {
    init {
        generic("Key", AllTypes.dataOnly)
        generic("Value", AllTypes.dataOnly)
        input("Signal", SignalType)
        genericInput("Dictionary")
        genericInput("Key")
        output("Next", SignalType)
    }

    private val cache = WeakHashMap<Pair<ValueType<*>, ValueType<*>>, Impl<*, *>>()
    override fun create(generics: Map<String, ValueType<*>>): Impl<*, *> = cache.computeIfAbsent(generics["Key"]!! to generics["Value"]!!) { Impl(generics["Key"]!!, generics["Value"]!!) }

    class Impl<K : Any, V : Any>(key: ValueType<K>, value: ValueType<V>) : BaseNode("Dictionary(${key.name}, ${value.name}) Remove", key.material) {
        private val signal = input("Signal", SignalType)
        private val dict = input("Dictionary", DictionaryType.create(key, value))
        private val keyIn = input("Key", key)
        private val next = output("Next", SignalType)

        override val generic = DictionaryRemoveNode
        override val generics = mapOf("Key" to key, "Value" to value)

        override fun setup(ctx: NodeContext) {
            ctx[signal].signalListener = {
                it[ctx[keyIn]]?.let { key ->
                    it[ctx[dict]]?.store?.remove(key)
                }
                it.emit(ctx[next])
            }
        }
    }
}

object DictionarySizeNode : GenericNode("Dictionary Size", Material.YELLOW_WOOL) {
    init {
        generic("Key", AllTypes.dataOnly)
        generic("Value", AllTypes.dataOnly)
        genericInput("Dictionary")
        output("Size", NumberType)
    }

    private val cache = WeakHashMap<Pair<ValueType<*>, ValueType<*>>, Impl<*, *>>()
    override fun create(generics: Map<String, ValueType<*>>): Impl<*, *> = cache.computeIfAbsent(generics["Key"]!! to generics["Value"]!!) { Impl(generics["Key"]!!, generics["Value"]!!) }

    class Impl<K : Any, V : Any>(key: ValueType<K>, value: ValueType<V>) : BaseNode("Dictionary(${key.name}, ${value.name}) Size", key.material) {
        private val dict = input("Dictionary", DictionaryType.create(key, value))
        private val size = output("Size", NumberType)

        override val generic = DictionarySizeNode
        override val generics = mapOf("Key" to key, "Value" to value)

        override fun setup(ctx: NodeContext) {
            ctx[size].defaultHandler = {
                it[ctx[dict]]?.store?.size?.toDouble()
            }
        }
    }
}


object DictionaryKeysNode : GenericNode("Dictionary Keys", Material.GREEN_WOOL) {
    init {
        generic("Key", AllTypes.dataOnly)
        generic("Value", AllTypes.dataOnly)
        genericInput("Dictionary")
        genericInput("Keys")
    }

    private val cache = WeakHashMap<Pair<ValueType<*>, ValueType<*>>, Impl<*, *>>()
    override fun create(generics: Map<String, ValueType<*>>): Impl<*, *> = cache.computeIfAbsent(generics["Key"]!! to generics["Value"]!!) { Impl(generics["Key"]!!, generics["Value"]!!) }

    class Impl<K : Any, V : Any>(private val key: ValueType<K>, value: ValueType<V>) : BaseNode("Dictionary(${key.name}, ${value.name}) Keys", key.material) {
        private val dict = input("Dictionary", DictionaryType.create(key, value))
        private val keys = output("Keys", ListType.create(key))

        override val generic = DictionarySizeNode
        override val generics = mapOf("Key" to key, "Value" to value)

        override fun setup(ctx: NodeContext) {
            ctx[keys].defaultHandler = { evalCtx ->
                evalCtx[ctx[dict]]?.store?.keys?.let { ListReference(key, it.toMutableList()) }
            }
        }
    }
}


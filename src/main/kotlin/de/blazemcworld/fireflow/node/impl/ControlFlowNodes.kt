package de.blazemcworld.fireflow.node.impl

import de.blazemcworld.fireflow.node.*
import net.minestom.server.MinecraftServer
import net.minestom.server.item.Material
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule
import java.util.*

object ScheduleNode : BaseNode("Schedule", Material.CLOCK) {
    private val signal = input("Signal", SignalType)
    private val delay = input("Delay", NumberType, optional=true)
    private val sharedLocals = input("Shared Locals", ConditionType, optional=true)
    private val next = output("Next", SignalType)
    private val schedule = output("Schedule", SignalType)

    override fun setup(ctx: NodeContext) {
        ctx[signal].signalListener = {
            var task: Task? = null
            val stop: () -> Unit = {
                task?.cancel()
            }
            ctx.global.onDestroy += stop
            val child = it.child(it[ctx[sharedLocals]] ?: false)
            task = MinecraftServer.getSchedulerManager().scheduleTask({
                ctx.global.onDestroy -= stop
                child.emit(ctx[schedule], now = true)
                return@scheduleTask TaskSchedule.stop()
            }, TaskSchedule.tick(it[ctx[delay]]?.toInt() ?: 1))

            it.emit(ctx[next])
        }
    }
}

object ForEachNode : GenericNode("For Each", Material.GREEN_WOOL) {
    init {
        generic("Type", AllTypes.dataOnly)
        input("Signal", SignalType)
        genericInput("List")
        output("For Each", SignalType)
        genericOutput("Current")
        output("Next", SignalType)
    }

    private val cache = WeakHashMap<ValueType<*>, Impl<*>>()
    override fun create(generics: Map<String, ValueType<*>>): Impl<*> = cache.computeIfAbsent(generics["Type"]) { Impl(generics["Type"]!!) }

    class Impl<T : Any>(private val type: ValueType<T>) : BaseNode("For Each ${type.name}", type.material) {
        private val signal = input("Signal", SignalType)
        private val list = input("List", ListType.create(type))
        private val forEach = output("For Each", SignalType)
        private val current = output("Current", type)
        private val next = output("Next", SignalType)

        override val generic = ForEachNode
        override val generics = mapOf("Type" to type)

        override fun setup(ctx: NodeContext) {
            ctx[signal].signalListener = {
                it[ctx[list]]?.store?.let { l -> ArrayList(l) }?.forEach { v ->
                    val typed = type.validate(v) ?: return@forEach
                    it[ctx[current]] = { typed }
                    it.emit(ctx[forEach])
                }
                it.emit(ctx[next])
            }
        }

    }
}
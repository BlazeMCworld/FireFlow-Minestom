package de.blazemcworld.fireflow.node

import de.blazemcworld.fireflow.Config
import de.blazemcworld.fireflow.FireFlow
import de.blazemcworld.fireflow.Lobby
import de.blazemcworld.fireflow.gui.NodeComponent
import de.blazemcworld.fireflow.space.Space
import de.blazemcworld.fireflow.util.sendError
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.timer.TaskSchedule

class GlobalNodeContext(val space: Space) {
    val onDestroy = mutableSetOf<() -> Unit>()
    val nodeContexts = mutableMapOf<NodeComponent, NodeContext>()
    val varStore = mutableMapOf<String, Any>()
    private var cpuTime = 0L
    private var cpuDepth = 0
    private var cpuStart = System.nanoTime()
    private var halted = false

    init {
        for (component in space.codeNodes) nodeContexts[component] = NodeContext(this, component)
        for (ctx in nodeContexts.values) ctx.computeConnections()
        for (component in space.codeNodes) {
            try {
                component.node.setup(nodeContexts[component]!!)
            } catch (e: Exception) {
                FireFlow.LOGGER.error(e) { "Error setting up node ${component.node}" }
            }
        }

        val resetCpu = MinecraftServer.getSchedulerManager().submitTask {
            if (!space.playInstance.isRegistered) return@submitTask TaskSchedule.stop()
            cpuTime = 0
            return@submitTask TaskSchedule.tick(1)
        }
        onDestroy += { resetCpu.cancel() }
    }

    fun measureCode(code: () -> Unit) {
        if (cpuDepth == 0) cpuStart = System.nanoTime()
        if (cpuLimit()) return halt()
        cpuDepth++
        try { code() } finally {
            cpuDepth--
            if (cpuDepth == 0) {
                cpuTime += System.nanoTime() - cpuStart
                if (cpuTime > Config.store.limits.cpuPerTick) halt()
            }
        }
    }

    private fun halt() {
        onDestroy.forEach { it() }
        halted = true
        for (p in space.playInstance.players) {
            p.sendMessage(Component.text("Space was halted for using too much cpu!").color(NamedTextColor.RED))
            Lobby.playerJoin(p)
        }
        for (player in space.codeInstance.players) {
            player.sendError("This space has been halted for using too much cpu!")
        }
        FireFlow.LOGGER.info { "Halted Space #${space.id}! (Used ${cpuTime}ns CPU, ${Config.store.limits.cpuPerTick}ns Limit)"}
    }

    fun cpuLimit() = cpuTime + System.nanoTime() - cpuStart > Config.store.limits.cpuPerTick
}
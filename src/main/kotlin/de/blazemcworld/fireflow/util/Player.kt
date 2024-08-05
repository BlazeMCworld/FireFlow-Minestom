package de.blazemcworld.fireflow.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.attribute.Attribute
import net.minestom.server.event.trait.PlayerInstanceEvent
import net.minestom.server.instance.Instance

class PlayerExitInstanceEvent(private val thePlayer: Player, private val theInstance: Instance) : PlayerInstanceEvent {
    override fun getInstance() = theInstance
    override fun getPlayer() = thePlayer
}

fun Player.reset() {
    gameMode = GameMode.ADVENTURE
    isFlying = false
    isAllowFlying = false
    inventory.clear()

    getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE).baseValue = 4.5
}

fun Player.sendError(msg: String) {
    sendMessage(Component.text(msg).color(NamedTextColor.RED))
}

fun Player.fireflowSetInstance(instance: Instance, pos: Pos = Pos.ZERO) {
    MinecraftServer.getGlobalEventHandler().call(PlayerExitInstanceEvent(this, this.instance))
    setInstance(instance, pos)
    instance.chunks.forEach { if (this in it.viewers) sendChunk(it) }
}
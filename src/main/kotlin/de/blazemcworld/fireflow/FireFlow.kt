package de.blazemcworld.fireflow

import de.blazemcworld.fireflow.command.Commands
import de.blazemcworld.fireflow.database.DatabaseHelper
import de.blazemcworld.fireflow.node.impl.NodeList
import de.blazemcworld.fireflow.util.PlayerExitInstanceEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.GameMode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.server.ServerListPingEvent
import net.minestom.server.extras.MojangAuth
import net.minestom.server.ping.ResponseData
import java.io.File
import java.util.*

object FireFlow {
    val LOGGER = KotlinLogging.logger {}
}

fun main() {
    FireFlow.LOGGER.info { "Starting up FireFlow..." }

    DatabaseHelper.init()

    val srv = MinecraftServer.init()!!
    MinecraftServer.setBrandName("FireFlow")
    MojangAuth.init()

    Lobby
    Commands

    val events = MinecraftServer.getGlobalEventHandler()

    events.addListener(AsyncPlayerConfigurationEvent::class.java) {
        it.spawningInstance = Lobby.instance
        it.player.gameMode = GameMode.ADVENTURE
        DatabaseHelper.onJoin(it.player)
    }
    events.addListener(PlayerDisconnectEvent::class.java) {
        MinecraftServer.getGlobalEventHandler().call(PlayerExitInstanceEvent(it.player, it.player.instance))
    }

    events.addListener(ServerListPingEvent::class.java) {
        val res = ResponseData()
        res.description = MiniMessage.miniMessage().deserialize(Config.store.motd)

        val file = File("favicon.png")
        if (file.exists()) res.favicon = "data:image/png;base64," + Base64.getEncoder().encodeToString(file.readBytes())

        it.responseData = res
    }

    Thread(ConsoleHandler::run).start()

    srv.start("0.0.0.0", Config.store.port)

    FireFlow.LOGGER.info { "Found ${NodeList.all.size} Node Implementations." }
    FireFlow.LOGGER.info { "Ready!" }
}

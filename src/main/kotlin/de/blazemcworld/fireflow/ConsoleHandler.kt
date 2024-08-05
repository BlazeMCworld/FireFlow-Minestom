package de.blazemcworld.fireflow

import net.minestom.server.MinecraftServer
import java.net.URI
import java.util.*
import java.util.zip.ZipInputStream
import kotlin.io.path.Path
import kotlin.io.path.writeBytes

object ConsoleHandler {
    fun run() {
        val scanner = Scanner(System.`in`)

        while (scanner.hasNextLine()) {
            val input = scanner.nextLine()

            when (input) {
                "update" -> {
                    FireFlow.LOGGER.info { "Updating..." }
                    ZipInputStream(URI.create(Config.store.updateUrl).toURL().openStream()).use {
                        var entry = it.nextEntry
                        while (entry?.name?.endsWith(".jar") == false) {
                            it.closeEntry()
                            entry = it.nextEntry
                        }
                        if (entry == null) {
                            FireFlow.LOGGER.info { "Failed to find jar in zip!" }
                            return@use
                        }
                        val self = Path(ConsoleHandler::class.java.protectionDomain.codeSource.location.toURI().path)
                        self.writeBytes(it.readAllBytes())
                    }
                    FireFlow.LOGGER.info { "Updated jar!" }
                }
                "stop" -> break
                else -> FireFlow.LOGGER.info { "Unknown command!" }
            }
        }

        MinecraftServer.stopCleanly()
    }
}
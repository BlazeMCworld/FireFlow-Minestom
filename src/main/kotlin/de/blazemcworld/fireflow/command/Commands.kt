package de.blazemcworld.fireflow.command

import net.minestom.server.MinecraftServer

object Commands {
    init {
        val all = listOf(
            CodeCommand,
            ContributorCommand,
            FunctionCommand,
            JoinCommand,
            LiteralCommand,
            LobbyCommand,
            PlayCommand,
            ReloadCommand,
            VariablesCommand,
            InsetCommand
        )
        all.forEach(MinecraftServer.getCommandManager()::register)
    }
}
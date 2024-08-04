package de.blazemcworld.fireflow.command

import de.blazemcworld.fireflow.gui.IOComponent
import de.blazemcworld.fireflow.space.SpaceManager
import de.blazemcworld.fireflow.util.sendError
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandExecutor
import net.minestom.server.command.builder.arguments.ArgumentString
import net.minestom.server.entity.Player

object InsetCommand : Command("inset") {
    init {
        addSyntax(CommandExecutor exec@{ sender, ctx ->
            if (sender !is Player) return@exec

            val space = SpaceManager.currentSpace(sender)

            if (space == null || space.codeInstance != sender.instance) {
                sender.sendError("You must be coding to use this!")
                return@exec
            }

            val cursor = space.codeCursor(sender)
            var done = false
            space.codeNodes.find { it.includes(cursor) }?.let {
                for (input in it.inputs) {
                    if (input.includes(cursor)) {
                        done = true
                        if (input is IOComponent.InsetInput<*>) {
                            val literal = input.io.type.parse(ctx.get("value"), space)
                            if (literal == null) {
                                sender.sendError("Value seems invalid!")
                                return@let true
                            }
                            input.updateInset(ctx.get("value"), space)
                            input.node.update(space.codeInstance)

                            input.disconnectAll()

                            return@let true
                        } else {
                            println("it should error")
                            sender.sendError("This input type does not support inset literals.")
                            return@let true
                        }
                    }
                }
            }
            if (!done) sender.sendError("You must be looking at and input that supports inset literals!")
        }, ArgumentString("value"))
    }
}
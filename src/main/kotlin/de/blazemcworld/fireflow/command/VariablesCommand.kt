package de.blazemcworld.fireflow.command

import de.blazemcworld.fireflow.database.table.PlayersTable
import de.blazemcworld.fireflow.database.table.SpaceRolesTable
import de.blazemcworld.fireflow.node.ValueType
import de.blazemcworld.fireflow.space.SpaceManager
import de.blazemcworld.fireflow.util.sendError
import net.kyori.adventure.text.Component
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandExecutor
import net.minestom.server.command.builder.arguments.ArgumentString
import net.minestom.server.entity.Player
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object VariablesCommand : Command("variables", "vars") {
    init {
        defaultExecutor = CommandExecutor exec@{ sender, ctx ->
            if (sender !is Player) return@exec

            val space = SpaceManager.currentSpace(sender)

            if (space == null) {
                sender.sendError("You need to be in a space to do this!")
                return@exec
            }

            val role = transaction {
                val result = SpaceRolesTable.join(PlayersTable, JoinType.INNER, SpaceRolesTable.player, PlayersTable.id)
                    .selectAll().where((SpaceRolesTable.space eq space.id) and (PlayersTable.uuid eq sender.uuid))
                    .adjustSelect { select(SpaceRolesTable.role) }
                if (result.empty()) return@transaction null
                result.single()[SpaceRolesTable.role]
            }

            if (role != SpaceRolesTable.Role.OWNER && role != SpaceRolesTable.Role.CONTRIBUTOR) {
                sender.sendError("You are not allowed to do that!")
                return@exec
            }

            var msgs = 0
            for ((name, value) in space.varStore) {
                if (!name.contains(ctx.get("filter") ?: "")) continue
                sender.sendMessage(Component.text(name + " -> " + (value.first as ValueType<Any>).stringify(value.second)).color(value.first.color))
                if (msgs++ > 100) break
            }
        }

        addSyntax(defaultExecutor!!, ArgumentString("filter"))
    }
}
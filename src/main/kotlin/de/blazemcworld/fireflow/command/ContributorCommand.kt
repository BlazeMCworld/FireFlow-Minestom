package de.blazemcworld.fireflow.command

import de.blazemcworld.fireflow.database.table.PlayersTable
import de.blazemcworld.fireflow.database.table.SpaceRolesTable
import de.blazemcworld.fireflow.space.SpaceManager
import de.blazemcworld.fireflow.util.fireflowSetInstance
import de.blazemcworld.fireflow.util.sendError
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentLiteral
import net.minestom.server.command.builder.arguments.ArgumentString
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity
import net.minestom.server.entity.Player
import net.minestom.server.utils.entity.EntityFinder
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction

object ContributorCommand : Command("contributor") {
    init {
        addSyntax(exec@{ sender, ctx ->
            if (sender !is Player) return@exec
            addContributor(sender, ctx.get("player"))
        }, ArgumentLiteral("add"), ArgumentString("player"))

        addSyntax(exec@{ sender, ctx ->
            if (sender !is Player) return@exec
            addContributor(sender, (ctx.get<EntityFinder>("player").findFirstPlayer(sender) ?: run {
                sender.sendError("No player by that name found! (Case sensitive)")
                return@exec
            }).username)
        }, ArgumentLiteral("add"), ArgumentEntity("player").onlyPlayers(true).singleEntity(true))

        addSyntax(exec@{ sender, ctx ->
            if (sender !is Player) return@exec
            removeContributor(sender, ctx.get("player"))
        }, ArgumentLiteral("remove"), ArgumentString("player"))

        addSyntax(exec@{ sender, _ ->
            if (sender !is Player) return@exec
            val space = SpaceManager.currentSpace(sender) ?: run {
                sender.sendError("You need to be in a space to do this!")
                return@exec
            }

            val roles = mutableMapOf<SpaceRolesTable.Role, String>()
            transaction {
                SpaceRolesTable.join(PlayersTable, JoinType.INNER, SpaceRolesTable.player, PlayersTable.id)
                    .selectAll().where(SpaceRolesTable.space eq space.id)
                    .adjustSelect { select(PlayersTable.name, SpaceRolesTable.role) }.forEach {
                        val role = it[SpaceRolesTable.role]
                        if (roles[role] == null) roles[role] = it[PlayersTable.name]
                        else roles[role] += ", " + it[PlayersTable.name]
                }
            }

            for (role in SpaceRolesTable.Role.entries) {
                if (!roles.containsKey(role)) continue
                sender.sendMessage(Component.text((role.title ?: continue) + ":").color(NamedTextColor.AQUA))
                sender.sendMessage(Component.text(roles[role]!!).color(NamedTextColor.GRAY))
            }
        }, ArgumentLiteral("list"))
    }

    private fun addContributor(owner: Player, name: String) {
        val space = SpaceManager.currentSpace(owner) ?: run {
            owner.sendError("You need to be in a space to do this!")
            return
        }

        val role = transaction {
            val result = SpaceRolesTable.join(PlayersTable, JoinType.INNER, SpaceRolesTable.player, PlayersTable.id)
                .selectAll().where((SpaceRolesTable.space eq space.id) and (PlayersTable.uuid eq owner.uuid))
                .adjustSelect { select(SpaceRolesTable.role) }
            if (result.empty()) return@transaction null
            result.single()[SpaceRolesTable.role]
        }

        if (role != SpaceRolesTable.Role.OWNER) {
            owner.sendError("You are not allowed to do that!")
            return
        }

        transaction {
            if (!SpaceRolesTable.join(PlayersTable, JoinType.INNER, SpaceRolesTable.player, PlayersTable.id).selectAll()
                    .where { (SpaceRolesTable.space eq space.id) and (PlayersTable.name eq name) }
                    .empty()) {
                owner.sendError("That player already is a contributor!")
                return@transaction
            }

            val contributor = PlayersTable.selectAll().where(PlayersTable.name eq name)

            if (contributor.empty()) {
                owner.sendError("No player by that name found! (Case sensitive)")
                return@transaction
            }

            SpaceRolesTable.insert {
                it[SpaceRolesTable.space] = space.id
                it[player] = contributor.adjustSelect { select(PlayersTable.id) }.single()[PlayersTable.id].value
                it[SpaceRolesTable.role] = SpaceRolesTable.Role.CONTRIBUTOR
            }
            owner.sendMessage(Component.text("$name now is a space contributor.").color(NamedTextColor.AQUA))
        }
    }

    private fun removeContributor(owner: Player, name: String) {
        val space = SpaceManager.currentSpace(owner) ?: run {
            owner.sendError("You need to be in a space to do this!")
            return
        }

        val role = transaction {
            val result = SpaceRolesTable.join(PlayersTable, JoinType.INNER, SpaceRolesTable.player, PlayersTable.id)
                .selectAll().where((SpaceRolesTable.space eq space.id) and (PlayersTable.uuid eq owner.uuid))
                .adjustSelect { select(SpaceRolesTable.role) }
            if (result.empty()) return@transaction null
            result.single()[SpaceRolesTable.role]
        }

        if (role != SpaceRolesTable.Role.OWNER) {
            owner.sendError("You are not allowed to do that!")
            return
        }

        val deleteMe = mutableSetOf<Int>()
        transaction {
            SpaceRolesTable.join(PlayersTable, JoinType.INNER, SpaceRolesTable.player, PlayersTable.id).selectAll()
                .where { (SpaceRolesTable.space eq space.id) and (PlayersTable.name eq name) }
                .adjustSelect { select(SpaceRolesTable.id, PlayersTable.name, PlayersTable.uuid) }.forEach {
                    deleteMe += it[SpaceRolesTable.id].value
                    owner.sendMessage(Component.text(it[PlayersTable.name] + " was removed from the space contributors.").color(NamedTextColor.AQUA))
                    space.codeInstance.getPlayerByUuid(it[PlayersTable.uuid])?.let { target ->
                        target.fireflowSetInstance(space.playInstance)
                        target.sendError("Your contributor status was removed!")
                    }
                }
            SpaceRolesTable.deleteWhere { SpaceRolesTable.id inList deleteMe  }
        }
        if (deleteMe.isNotEmpty()) return
        owner.sendError("No contributor by that name found (Case sensitive).")
    }
}
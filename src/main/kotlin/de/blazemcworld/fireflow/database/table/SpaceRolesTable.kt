package de.blazemcworld.fireflow.database.table

import de.blazemcworld.fireflow.database.SimpleIntSerialization
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.json.jsonb

object SpaceRolesTable : IntIdTable("space_roles") {
    val player = integer("player").references(PlayersTable.id)
    val space = integer("space").references(SpacesTable.id)
    val role = jsonb("role", Json, SimpleIntSerialization(
        to = { it.id },
        from = { Role.byId(it) }
    ))

    enum class Role(val id: Int, val title: String?) {
        UNKNOWN(0, null),
        OWNER(1, "Owner"),
        CONTRIBUTOR(2, "Contributor");

        companion object {
            private val id2roleMap = mutableMapOf<Int, Role>()
            fun byId(id: Int) = id2roleMap[id] ?: UNKNOWN

            init {
                for (role in entries) Role.id2roleMap[role.id] = role
            }
        }
    }
}
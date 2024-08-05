package de.blazemcworld.fireflow.database.table

import org.jetbrains.exposed.dao.id.IntIdTable

object PlayersTable : IntIdTable("players") {
    val name = varchar("name", 16)
    val uuid = uuid("uuid")
    val preferences = mutableMapOf(
        "reload" to byte("preference-reload").default(0),
        "auto-tools" to byte("preference-auto-tools").default(0),
        "code-control" to byte("preference-code-control").default(0),
        "delete-warning" to byte("preference-delete-warning").default(0)
    )
}
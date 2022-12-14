package dev.racci.elixir.api.data

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import java.util.UUID

public abstract class PlayerRelationTable<T>(
    relatedTable: IdTable<T>,
    name: String = ""
) : IdTable<UUID>(name) where T : Comparable<T> {
    public val relational: Column<EntityID<T>> = reference("related", relatedTable)

    final override val id: Column<EntityID<UUID>> = uuid("uuid").entityId()
    final override val primaryKey: PrimaryKey = PrimaryKey(relational, id)
}

public abstract class PlayerRelatedEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    public val elixirPlayer: ElixirPlayer by lazy { ElixirPlayer.findById(id.value)!! }
}

public abstract class PlayerRelatedEntityClass<out E : PlayerRelatedEntity>(
    table: PlayerRelationTable<*>,
    entityType: Class<E>? = null,
    entityCtor: ((EntityID<UUID>) -> E)? = null
) : EntityClass<UUID, E>(table, entityType, entityCtor) {
    override fun new(id: UUID?, init: E.() -> Unit): E = super.new(
        if (id !is UUID || ElixirPlayer.findById(id) == null) {
            ElixirPlayer.new(id) {}.id.value
        } else id,
        init
    )
}

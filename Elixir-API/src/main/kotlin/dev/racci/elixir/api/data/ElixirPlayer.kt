package dev.racci.elixir.api.data

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import java.util.UUID

public class ElixirPlayer private constructor(uuid: EntityID<UUID>) : UUIDEntity(uuid) {
    public companion object : UUIDEntityClass<ElixirPlayer>(ElixirPlayers)

    private var _joinMessage by ElixirPlayers.joinMessage
    private var _leaveMessage by ElixirPlayers.leaveMessage
    private var _purchases by ElixirPlayers.purchases

    public var disableConnectionMessages: Boolean by ElixirPlayers.disableConnectionMessages

    public var joinMessage: Component?
        get() = deserialize(_joinMessage)
        set(value) {
            _joinMessage = serialize(value)
        }

    public var leaveMessage: Component?
        get() = deserialize(_leaveMessage)
        set(value) {
            _leaveMessage = serialize(value)
        }

    public var opals: Int by ElixirPlayers.opals

    public val purchases: MutableMap<String, Int> by lazy(ElixirPlayer::PurchaseMap)

    public inner class PurchaseMap : MutableMap<String, Int> by (
        _purchases.split(",")
            .map { it.split(":", limit = 2) }
            .filter { it.size == 2 }
            .associate { it[0] to it[1].toInt() }.toMutableMap()
        ) {
        override fun put(key: String, value: Int): Int? {
            val old = get(key)
            val oldString = "$key:${old ?: 0}"
            val newString = "$key:$value"

            if (_purchases.contains(oldString)) {
                _purchases = _purchases.replace(oldString, newString)
            } else {
                if (_purchases.isNotEmpty()) _purchases += ","
                _purchases += newString
            }

            return old
        }

        override fun remove(key: String): Int? {
            val old = get(key)
            _purchases = _purchases.replace("$key:${old ?: 0}", "")
            return old
        }
    }

    private fun deserialize(value: String?): Component? {
        if (value == null) return null

        return GsonComponentSerializer.gson().deserialize(value)
    }

    private fun serialize(value: Component?): String? {
        if (value == null) return null

        return GsonComponentSerializer.gson().serialize(value)
    }

    @ApiStatus.Internal
    public object ElixirPlayers : UUIDTable("user") {
        public val joinMessage: Column<String?> = text("join_message").nullable().default(null)
        public val leaveMessage: Column<String?> = text("leave_message").nullable().default(null)
        public val disableConnectionMessages: Column<Boolean> = bool("disable_connection_messages").default(false)

        public val opals: Column<Int> = integer("opals").default(0)
        public val purchases: Column<String> = text("purchases").default("")
    }
}

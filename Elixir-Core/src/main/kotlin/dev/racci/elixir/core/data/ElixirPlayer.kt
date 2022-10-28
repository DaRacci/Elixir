package dev.racci.elixir.core.data

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.UUID

class ElixirPlayer(uuid: EntityID<UUID>) : UUIDEntity(uuid) {
    private var _joinMessage by ElixirUser.joinMessage
    private var _leaveMessage by ElixirUser.leaveMessage
    private var _purchases by ElixirUser.purchases

    var disableConnectionMessages by ElixirUser.disableConnectionMessages

    var joinMessage: Component?
        get() = deserialize(_joinMessage)
        set(value) {
            _joinMessage = serialize(value)
        }

    var leaveMessage: Component?
        get() = deserialize(_leaveMessage)
        set(value) {
            _leaveMessage = serialize(value)
        }

    var opals: Int by ElixirUser.opals

    val purchases: MutableMap<String, Int> by lazy(::PurchaseMap)

    inner class PurchaseMap : MutableMap<String, Int> by (
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

    internal object ElixirUser : UUIDTable("user") {
        val joinMessage = text("join_message").nullable().default(null)
        val leaveMessage = text("leave_message").nullable().default(null)
        val disableConnectionMessages = bool("disable_connection_messages").default(false)

        val opals = integer("opals").default(0)
        val purchases = text("purchases").default("")
    }

    companion object : UUIDEntityClass<ElixirPlayer>(ElixirUser)
}

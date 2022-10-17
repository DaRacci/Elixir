package dev.racci.elixir.core.data

import dev.racci.elixir.core.Elixir
import dev.racci.minix.api.utils.Closeable
import dev.racci.minix.api.utils.getKoin
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import java.util.UUID
import java.util.concurrent.CompletableFuture

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

    inner class PurchaseMap : MutableMap<String, Int> by (_purchases.split(",").map { it.split(":", limit = 1) }.associate { it[0] to it[1].toInt() }.toMutableMap()) {
        override fun put(key: String, value: Int): Int? {
            val old = get(key)
            val oldString = "$key:${old ?: 0}"
            val newString = "$key:$value"

            if (_purchases.contains(oldString)) {
                _purchases = _purchases.replace(oldString, newString)
            } else _purchases += ",$newString"

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

    companion object : UUIDEntityClass<ElixirPlayer>(ElixirUser) {
        @OptIn(DelicateCoroutinesApi::class)
        val threadContext = object : Closeable<ExecutorCoroutineDispatcher>() {
            override fun create(): ExecutorCoroutineDispatcher = newSingleThreadContext("Elixir H2 Handler")

            override fun onClose() { this.value.value?.close() }
        }

        fun <T> transactionFuture(statement: suspend Transaction.() -> T): CompletableFuture<T> {
            return runBlocking { transactionDeferred(statement).asCompletableFuture() }
        }

        suspend fun <T> transactionDeferred(statement: suspend Transaction.() -> T): Deferred<T> {
            val db = getKoin().getProperty<Database>(Elixir.KOIN_DATABASE) ?: throw IllegalStateException("Database is not initialized")

            return suspendedTransactionAsync(threadContext.get(), db) {
                this.addLogger(StdOutSqlLogger)
                val result = statement()
                commit()
                result
            }
        }
    }
}

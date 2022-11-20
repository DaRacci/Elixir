package dev.racci.elixir.core.services

import dev.racci.elixir.core.Elixir
import dev.racci.elixir.core.data.ElixirPlayer
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.services.StorageService
import dev.racci.minix.api.utils.getKoin
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction

@MappedExtension(Elixir::class, "Elixir Storage Service")
public class ElixirStorageService(override val plugin: Elixir) : StorageService<Elixir>, Extension<Elixir>() {
    override val managedTable: Table = ElixirPlayer.ElixirUser

    public companion object {
        public fun <T> transaction(
            statement: Transaction.() -> T
        ): T {
            var result: T? = null
            runBlocking {
                getKoin().get<ElixirStorageService>().withDatabase {
                    result = statement()
                }
            }

            return result!!
        }
    }
}

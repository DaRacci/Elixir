package dev.racci.elixir.core.services

import dev.racci.elixir.core.Elixir
import dev.racci.elixir.core.data.ElixirPlayer
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.services.StorageService
import org.jetbrains.exposed.sql.Table

@MappedExtension(Elixir::class, "Elixir Storage Service")
class ElixirStorageService(override val plugin: Elixir) : StorageService<Elixir>, Extension<Elixir>() {
    override val managedTable: Table = ElixirPlayer.ElixirUser
}

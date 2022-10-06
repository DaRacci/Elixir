package dev.racci.elixir.core

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.racci.elixir.core.data.ElixirPlayer
import dev.racci.elixir.core.modules.AetherModule
import dev.racci.elixir.core.modules.BeaconModule
import dev.racci.elixir.core.modules.ConcreteModule
import dev.racci.elixir.core.modules.ConnectionMessageModule
import dev.racci.elixir.core.modules.TorchFireModule
import dev.racci.minix.api.annotations.MappedPlugin
import dev.racci.minix.api.plugin.MinixPlugin
import me.angeschossen.lands.api.exceptions.FlagConflictException
import me.angeschossen.lands.api.flags.Flag
import me.angeschossen.lands.api.flags.types.LandFlag
import me.angeschossen.lands.api.integration.LandsIntegration
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

@MappedPlugin(-1, Elixir::class)
class Elixir : MinixPlugin() {
    override suspend fun handleLoad() {
        this.registerLandsFlag()
    }

    override suspend fun handleEnable() {
        this.prepareDatabase()

        AetherModule.tryLoad()
        BeaconModule.tryLoad()
        ConcreteModule.tryLoad()
        TorchFireModule.tryLoad()
        ConnectionMessageModule.tryLoad()
    }

    override suspend fun handleDisable() {
        getKoin().getProperty<HikariDataSource>(KOIN_DATASOURCE)?.close()
        getKoin().deleteProperty(KOIN_DATASOURCE)
        getKoin().deleteProperty(KOIN_DATABASE)
    }

    private fun prepareDatabase() {
        val config = HikariConfig().apply {
            this.jdbcUrl = "jdbc:sqlite:${dataFolder.path}/database.db"
            this.connectionTestQuery = "SELECT 1"
            this.addDataSourceProperty("cachePrepStmts", true)
            this.addDataSourceProperty("prepStmtCacheSize", "250")
            this.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }
        val dataSource = HikariDataSource(config)
        val database = Database.connect(dataSource)
        getKoin().setProperty(KOIN_DATABASE, database)
        getKoin().setProperty(KOIN_DATASOURCE, dataSource)

        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(ElixirPlayer.User)
        }
    }

    private fun registerLandsFlag() {
        try {
            LandsIntegration(this)
                .registerFlag(
                    LandFlag(
                        this,
                        Flag.Target.PLAYER,
                        "PreventCoralDecay",
                        true,
                        false
                    ).apply {
                        defaultState = true
                        description = listOf("Prevent coral from naturally becoming dead coral when out of water.")
                        this.module
                        setIcon(ItemStack(Material.DEAD_BRAIN_CORAL))
                        setDisplayName("Prevent Coral Decay")
                    }
                )
        } catch (e: FlagConflictException) {
            log.error { "Flag conflict: ${e.existing.name} from plugin ${e.existing.plugin.description.fullName}" }
        } catch (e: IllegalStateException) {
            /* Elixir was loaded after server start. */
        }
    }

    companion object {
        const val KOIN_DATASOURCE = "elixir:dataSource"
        const val KOIN_DATABASE = "elixir:database"
    }
}

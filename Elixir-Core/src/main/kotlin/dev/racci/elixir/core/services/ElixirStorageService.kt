package dev.racci.elixir.core.services

import com.zaxxer.hikari.HikariDataSource
import dev.racci.elixir.api.data.ElixirPlayer
import dev.racci.elixir.api.data.challenge.Challenge
import dev.racci.elixir.api.data.challenge.ChallengeProgress
import dev.racci.elixir.core.Elixir
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.data.MinixConfig
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.plugin.logger.MinixLogger
import dev.racci.minix.api.services.DataService
import dev.racci.minix.api.services.StorageService
import dev.racci.minix.api.utils.getKoin
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlLogger
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.expandArgs
import javax.sql.DataSource

@MappedExtension(Elixir::class, "Elixir Storage Service")
public class ElixirStorageService(override val plugin: Elixir) : StorageService<Elixir>, Extension<Elixir>() {
    override val managedTable: Table = ElixirPlayer.ElixirPlayers

    override suspend fun getDatabase(): Database? {
        this.ensureSetup()
        return this.getProperty("database")
    }

    private suspend fun ensureSetup() {
        val database = this.getProperty<Database>("database")
        if (database != null) return

        createDatabaseConfig()
        this.setProperty("database", Database.connect(this.getProperty<DataSource>("dataSource")!!, databaseConfig = databaseConfig))

        withDatabase {
            val tables = arrayOf(ElixirPlayer.ElixirPlayers, Challenge.Challenges, ChallengeProgress.ChallengeProgression)
            SchemaUtils.create(*tables)
            SchemaUtils.addMissingColumnsStatements(*tables)
            SchemaUtils.createMissingTablesAndColumns(*tables)
        }
    }

    private fun createDatabaseConfig() {
        val storageConfig = DataService.getService().getMinixConfig(plugin).storage

        with(HikariDataSource()) {
            when (storageConfig.type) {
                MinixConfig.Minix.Storage.StorageType.SQLITE -> {
                    this.jdbcUrl = "jdbc:sqlite:${getStorageDirectory()}/database.db"
                }
                MinixConfig.Minix.Storage.StorageType.MARIADB -> {
                    this.driverClassName = "org.mariadb.jdbc.Driver"
                    this.jdbcUrl = "jdbc:mariadb://${storageConfig.host}:${storageConfig.port}/${storageConfig.database}"
                    this.username = storageConfig.username
                    this.password = storageConfig.password
                }
            }

            this@ElixirStorageService.getDataSourceProperties().forEach { (key, value) ->
                this.addDataSourceProperty(key, value)
            }

            setProperty("dataSource", this)
        }
    }

    public companion object {
        public fun <T> transaction(
            statement: suspend Transaction.() -> T
        ): T {
            var result: T? = null
            runBlocking {
                getKoin().get<ElixirStorageService>().withDatabase {
                    result = statement()
                }
            }

            return result!!
        }

        public val databaseConfig: DatabaseConfig = DatabaseConfig {
            this.sqlLogger = object : SqlLogger {
                override fun log(
                    context: StatementContext,
                    transaction: Transaction
                ) = getKoin().get<MinixLogger>().debug { "Executing SQL: ${context.expandArgs(transaction)}" }
            }
        }
    }
}

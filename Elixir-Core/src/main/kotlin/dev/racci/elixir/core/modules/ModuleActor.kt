package dev.racci.elixir.core.modules

import dev.racci.elixir.core.Elixir
import dev.racci.elixir.core.data.ElixirConfig
import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.utils.kotlin.ifTrue
import org.koin.core.component.get
import org.koin.core.component.inject
import kotlin.reflect.KProperty1

sealed class ModuleActor<M : ElixirConfig.Modules.ModuleConfig>(
    private val configPath: KProperty1<ElixirConfig.Modules, M>
) : WithPlugin<Elixir> {
    override val plugin: Elixir by inject()

    open suspend fun shouldLoad(): Boolean = getConfig().enabled

    abstract suspend fun load()

    suspend fun tryLoad() = this.shouldLoad().ifTrue {
        logger.debug { "Loading ${this::class.simpleName}" }
        this.load()
    }

    fun getConfig() = get<ElixirConfig>().modules[configPath.name].castOrThrow<M>()
}

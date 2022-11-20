package dev.racci.elixir.core.modules

import dev.racci.elixir.core.Elixir
import dev.racci.elixir.core.data.ElixirConfig
import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.utils.kotlin.ifTrue
import org.koin.core.component.get
import org.koin.core.component.inject

public sealed class ModuleActor<M : ElixirConfig.Modules.ModuleConfig> : WithPlugin<Elixir> {
    private val path: String by lazy { this::class.simpleName!!.replace(Regex("Module|Actor|_"), "").uppercase() }

    override val plugin: Elixir by inject()

    public open suspend fun shouldLoad(): Boolean = getConfig().enabled

    public abstract suspend fun load()

    public open suspend fun close(): Unit = Unit

    public suspend fun tryLoad(): Boolean = this.shouldLoad().ifTrue {
        logger.debug { "Loading ${this::class.simpleName}" }
        this.load()
    }

    protected fun getConfig(): M = get<ElixirConfig>().modules[path].castOrThrow<M>()
}

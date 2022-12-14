package dev.racci.elixir.core.modules

import cloud.commandframework.paper.PaperCommandManager
import cloud.commandframework.permission.Permission
import dev.racci.elixir.core.Elixir
import dev.racci.elixir.core.data.ElixirConfig
import dev.racci.minix.api.extensions.KListener
import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.utils.kotlin.ifTrue
import org.bukkit.command.CommandSender
import org.koin.core.component.get
import org.koin.core.component.inject

public sealed class ModuleActor<M : ElixirConfig.Modules.ModuleConfig> : WithPlugin<Elixir> {
    private val path: String by lazy { this::class.simpleName!!.replace(Regex("Module|Actor|_"), "").uppercase() }

    override val plugin: Elixir by inject()

    protected val modulePermission: Permission by lazy { Permission.of("elixir.${path.lowercase()}.command") }

    public open suspend fun shouldLoad(): Boolean = getConfig().enabled

    public open suspend fun load(): Unit = Unit

    public open suspend fun close(): Unit = Unit

    internal open suspend fun registerCommands(manager: PaperCommandManager<CommandSender>): Unit = Unit

    internal open suspend fun registerListeners(listener: KListener<Elixir>): Unit = Unit

    public suspend fun tryLoad(): Boolean = this.shouldLoad().ifTrue { this.load() }

    protected fun getConfig(): M = get<ElixirConfig>().modules[path].castOrThrow<M>()
}

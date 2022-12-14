package dev.racci.elixir.core.modules

import dev.racci.elixir.core.data.ElixirConfig
import org.koin.core.component.get

public open class ExperimentalActor<M : ElixirConfig.Modules.ModuleConfig> : ModuleActor<M>() {
    override suspend fun shouldLoad(): Boolean {
        return super.shouldLoad() && (plugin.version.isPreRelease || get<ElixirConfig>().enableExperimentalFeatures)
    }
}

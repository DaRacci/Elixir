package dev.racci.elixir.core.services

import com.github.benmanes.caffeine.cache.Caffeine
import dev.racci.elixir.core.Elixir
import dev.racci.elixir.core.modules.ModuleActor
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.KListener
import dev.racci.minix.api.extensions.unregisterListener
import io.github.classgraph.ClassGraph
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.inject

// TODO -> Auto add module to config when it's loaded.
@MappedExtension(Elixir::class, "Elixir Module Service", [CommandService::class])
public class ModuleService : Extension<Elixir>() {
    override val plugin: Elixir by inject()
    private val moduleCache = Caffeine.newBuilder()
        .evictionListener<ModuleActor<*>, KListener<Elixir>> { module, listener, _ ->
            async {
                module?.close()
                listener?.unregisterListener()
            }
        }.build<ModuleActor<*>, KListener<Elixir>> { object : KListener<Elixir> { override val plugin: Elixir get() = this@ModuleService.plugin } }

    override suspend fun handleEnable() {
        this.loadModules()
    }

    override suspend fun handleDisable() {
        moduleCache.invalidateAll()
    }

    private suspend fun loadModules() {
        val commandService by inject<CommandService>()

        ClassGraph().acceptPackages("dev.racci.elixir.core.modules")
            .addClassLoader(this::class.java.classLoader)
            .enableClassInfo()
            .scan()
            .use { scanResult ->
                scanResult.allClasses.asFlow()
                    .filter { info -> info.isFinal && !info.isSynthetic && info.isStandardClass && info.extendsSuperclass(ModuleActor::class.java) }
                    .mapNotNull { info -> info.loadClass().kotlin.objectInstance }
                    .filterIsInstance<ModuleActor<*>>()
                    .filter { actor -> actor.tryLoad() }
                    .catch { err -> logger.error(err) { "Failed to load module actor!" } }
                    .onEach { actor -> logger.info { "Loaded module actor ${actor::class.simpleName}" } }
                    .onEach { actor -> actor.registerListeners(moduleCache[actor]) }
                    .onEach { actor -> actor.registerCommands(commandService.manager.get()) }
                    .collect()
            }
    }
}

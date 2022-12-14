package dev.racci.elixir.core

import com.fren_gor.ultimateAdvancementAPI.AdvancementMain
import com.fren_gor.ultimateAdvancementAPI.UltimateAdvancementAPI
import com.willfp.eco.core.items.isEmpty
import dev.racci.minix.api.annotations.MappedPlugin
import dev.racci.minix.api.extensions.pdc
import dev.racci.minix.api.plugin.MinixPlugin
import dev.racci.minix.api.utils.loadModule
import dev.racci.tentacles.Tentacles
import me.angeschossen.lands.api.exceptions.FlagConflictException
import me.angeschossen.lands.api.flags.Flag
import me.angeschossen.lands.api.flags.types.LandFlag
import me.angeschossen.lands.api.integration.LandsIntegration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.incendo.interfaces.paper.PaperInterfaceListeners
import org.koin.core.component.get
import org.koin.dsl.bind

@MappedPlugin(-1, Elixir::class)
public class Elixir : MinixPlugin() {
    override suspend fun handleLoad() {
        this.registerLandsFlag()
        loadModule { single { AdvancementMain(this@Elixir).also(AdvancementMain::load) } bind AdvancementMain::class }

        if (tentaclesInstalled) {
            val multiToolKey = NamespacedKey(this, "multi-tool")
            Tentacles.addGlobalBlockDropCondition(multiToolKey) { player, state, itemStack ->
                if (itemStack.isEmpty || !itemStack!!.pdc.has(multiToolKey)) return@addGlobalBlockDropCondition null
                true
            }
        }
    }

    override suspend fun handleEnable() {
        PaperInterfaceListeners.install(this)
        get<AdvancementMain>().enableInMemory()
        loadModule { single { UltimateAdvancementAPI.getInstance(this@Elixir) } bind UltimateAdvancementAPI::class }
    }

    override suspend fun handleDisable() {
        get<AdvancementMain>().disable()
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
        } catch (_: IllegalStateException) {
            /* Elixir was loaded after server start. */
        } catch (_: NoClassDefFoundError) {
            /* Lands is not installed. */
        } catch (e: Exception) { // Should hopefully only be FlagConflictException
            e as FlagConflictException
            log.error { "Flag conflict: ${e.existing.name} from plugin ${e.existing.plugin.description.fullName}" }
        }
    }

    public companion object {
        public val tentaclesInstalled: Boolean = runCatching { Class.forName("dev.racci.tentacles.Tentacles") }.isSuccess
    }
}

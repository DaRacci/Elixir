package dev.racci.elixir.core

import com.willfp.eco.core.items.isEmpty
import dev.racci.elixir.core.modules.AetherModule
import dev.racci.elixir.core.modules.ConnectionMessageModule
import dev.racci.elixir.core.modules.DrownConcreteModule
import dev.racci.elixir.core.modules.EggTrackerModule
import dev.racci.elixir.core.modules.EnhanceBeaconsModule
import dev.racci.elixir.core.modules.HubModule
import dev.racci.elixir.core.modules.OpalsModule
import dev.racci.elixir.core.modules.TPSFixerModule
import dev.racci.elixir.core.modules.TerixModule
import dev.racci.elixir.core.modules.TorchFireModule
import dev.racci.minix.api.annotations.MappedPlugin
import dev.racci.minix.api.extensions.pdc
import dev.racci.minix.api.plugin.MinixPlugin
import dev.racci.tentacles.Tentacles
import me.angeschossen.lands.api.exceptions.FlagConflictException
import me.angeschossen.lands.api.flags.Flag
import me.angeschossen.lands.api.flags.types.LandFlag
import me.angeschossen.lands.api.integration.LandsIntegration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.incendo.interfaces.paper.PaperInterfaceListeners

@MappedPlugin(-1, Elixir::class)
public class Elixir : MinixPlugin() {
    override suspend fun handleLoad() {
        this.registerLandsFlag()

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

        AetherModule.tryLoad()
        EnhanceBeaconsModule.tryLoad()
        DrownConcreteModule.tryLoad()
        TorchFireModule.tryLoad()
        ConnectionMessageModule.tryLoad()
        TPSFixerModule.tryLoad()
        OpalsModule.tryLoad()
        EggTrackerModule.tryLoad()
        HubModule.tryLoad()
        TerixModule.tryLoad()
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

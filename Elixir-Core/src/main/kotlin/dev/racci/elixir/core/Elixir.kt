package dev.racci.elixir.core

import dev.racci.elixir.core.services.HookService
import dev.racci.elixir.core.services.StorageService
import dev.racci.minix.api.plugin.MinixPlugin
import me.angeschossen.lands.api.exceptions.FlagConflictException
import me.angeschossen.lands.api.flags.Flag
import me.angeschossen.lands.api.flags.types.LandFlag
import me.angeschossen.lands.api.integration.LandsIntegration
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.logging.Level
import kotlin.properties.Delegates

class Elixir : MinixPlugin() {

    lateinit var landsHook: LandsHook

    override suspend fun handleLoad() {
        if (description.version.endsWith("-SNAPSHOT")) logger.level = Level.ALL
        landsHook = LandsHook()
    }

    override suspend fun handleAfterLoad() {
        if (logger.level != Level.ALL && StorageService.getService()["debug"]) logger.level = Level.ALL
    }

    interface ILandsHook : HookService.HookService<LandsIntegration> {
        val coralDecayFlag: LandFlag
    }

    class LandsHook : ILandsHook {

        override var manager: LandsIntegration? = null
        override var coralDecayFlag: LandFlag by Delegates.notNull()

        init {
            manager = LandsIntegration(plugin)
            try {
                coralDecayFlag = LandFlag(plugin, Flag.Target.PLAYER, "PreventCoralDecay", true, false).apply {
                    defaultState = true
                    description = listOf("Prevent coral from naturally becoming dead coral when out of water.")
                    this.module
                    setIcon(ItemStack(Material.DEAD_BRAIN_CORAL))
                    setDisplayName("Prevent Coral Decay")
                }
                manager!!.registerFlag(coralDecayFlag)
            } catch (ex: FlagConflictException) {
                log.error("Flag conflict: ${ex.existing.name} from plugin ${ex.existing.plugin.description.fullName}")
            }
        }
    }
}

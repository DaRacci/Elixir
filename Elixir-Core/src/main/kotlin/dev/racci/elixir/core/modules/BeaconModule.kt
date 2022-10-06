package dev.racci.elixir.core.modules

import com.destroystokyo.paper.event.block.BeaconEffectEvent
import dev.racci.elixir.core.data.ElixirConfig
import dev.racci.minix.api.extensions.event
import org.bukkit.event.EventPriority
import org.bukkit.potion.PotionEffect

class BeaconModule : ModuleActor<ElixirConfig.Modules.EnhanceBeacon>(ElixirConfig.Modules::enhanceBeacons) {
    override suspend fun load() {
        event<BeaconEffectEvent>(EventPriority.HIGHEST, true) {
            if (!getConfig().removeParticles) return@event

            effect = PotionEffect(
                effect.type,
                effect.duration,
                effect.amplifier,
                true,
                false,
                false
            )
        }
    }
}

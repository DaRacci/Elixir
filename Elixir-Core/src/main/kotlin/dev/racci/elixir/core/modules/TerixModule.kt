package dev.racci.elixir.core.modules

import dev.racci.elixir.core.Elixir
import dev.racci.elixir.core.data.ElixirConfig
import dev.racci.minix.api.extensions.KListener
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.integrations.regions.RegionManager
import dev.racci.minix.api.utils.minecraft.asBlockPos
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityAirChangeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerTeleportEvent

public object TerixModule : ModuleActor<ElixirConfig.Modules.Terix>() {
    override suspend fun registerListeners(listener: KListener<Elixir>) {
        listener.event<EntityAirChangeEvent>(EventPriority.HIGHEST, true) {
            (this.entity as? LivingEntity)?.ifWithinProtectedRegion(this::cancel)
        }

        listener.event<PlayerTeleportEvent>(EventPriority.MONITOR, true) { this.player.maybeResetAir() }
        listener.event<PlayerJoinEvent>(EventPriority.MONITOR, true) { this.player.maybeResetAir() }
    }

    private fun LivingEntity.ifWithinProtectedRegion(action: () -> Unit) {
        RegionManager.getRegion(this.location.asBlockPos(), this.world)
            .filter { region -> region.name in getConfig().protectedRegions }
            .ifPresent { _ -> action() }
    }

    private fun LivingEntity.maybeResetAir() {
        if (this.remainingAir >= this.maximumAir) return
        this.ifWithinProtectedRegion { this.remainingAir = this.maximumAir }
    }
}

package dev.racci.elixir.core.modules

import dev.racci.elixir.core.data.ElixirConfig
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.integrations.regions.RegionManager
import dev.racci.minix.api.utils.minecraft.asBlockPos
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityAirChangeEvent

object TerixModule : ModuleActor<ElixirConfig.Modules.Terix>() {
    override suspend fun load() {
        event<EntityAirChangeEvent>(EventPriority.HIGHEST, true) {
            RegionManager.getRegion(this.entity.location.asBlockPos(), this.entity.world)
                .filter { region -> region.name in getConfig().protectedRegions }
                .ifPresent { _ -> this.cancel() }
        }
    }
}

package dev.racci.elixir.core.services

import dev.racci.elixir.core.Elixir
import dev.racci.elixir.core.data.ElixirPlayer
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.event
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.event.player.AsyncPlayerPreLoginEvent

@MappedExtension(Elixir::class, "Listener Service")
public class ListenerService(override val plugin: Elixir) : Extension<Elixir>() {
    override suspend fun handleEnable() {
        event<BlockFadeEvent>(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
        ) {
            if (!block.type.name.contains("CORAL") || !newState.type.name.startsWith("DEAD")) return@event
//            if (RegionManager.REGISTERED.all { it.pluginName != "Lands" }) return@event

//            val manager = hook.manager ?: return@event
//            val land = manager.getLand(block.location) ?: return@event
//            val area = land.getArea(block.location) ?: return@event
//            if (area.hasFlag(hook.coralDecayFlag)) cancel()
        }

        event<AsyncPlayerPreLoginEvent>(EventPriority.LOWEST) {
            val uuid = this.uniqueId
            try {
                ElixirStorageService.transaction {
                    ElixirPlayer.findById(uuid) ?: ElixirPlayer.new(uuid) {}
                }
            } catch (e: Exception) {
                logger.error(e) { "Error while fetching player from database: ${e.message}" }
            }
        }
    }
}

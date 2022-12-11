package dev.racci.elixir.core.modules

import com.Zrips.CMI.CMI
import dev.racci.elixir.core.constants.ElixirPermission
import dev.racci.elixir.core.data.ElixirConfig
import dev.racci.elixir.core.data.ElixirLang
import dev.racci.elixir.api.data.ElixirPlayer
import dev.racci.elixir.core.services.ElixirStorageService
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.message
import dev.racci.minix.api.extensions.scheduler
import dev.racci.minix.api.extensions.server
import net.kyori.adventure.text.Component
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.get
import kotlin.time.Duration.Companion.milliseconds

public object ConnectionMessageModule : ModuleActor<ElixirConfig.Modules.ConnectionMessage>() {

    override suspend fun load() {
        event<PlayerJoinEvent>(EventPriority.MONITOR, true) {
            joinMessage(null)
            scheduler {
                message(player) { this.joinMessage ?: get<ElixirLang>().defaultJoinMessage["player" to { player.displayName() }] }?.message(server)
            }.runAsyncTaskLater(plugin, 250.milliseconds) // Wait for Carbon to change the player's display name
        }

        event<PlayerQuitEvent>(EventPriority.MONITOR, true) {
            this.quitMessage(message(player) { this.leaveMessage ?: get<ElixirLang>().defaultLeaveMessage["player" to { player.displayName() }] })
        }
    }

    private fun message(
        player: Player,
        message: ElixirPlayer.() -> Component
    ): Component? {
        if (!player.hasPermission(ElixirPermission.CONNECTION_MESSAGE.permissionString)) {
            logger.debug { "Player ${player.name} does not have permission to send connection messages." }
            return null
        }

        return ElixirStorageService.transaction {
            val elixirPlayer = ElixirPlayer[player.uniqueId]
            if (elixirPlayer.disableConnectionMessages) {
                logger.debug { "Player ${player.name} has disabled connection messages." }
                return@transaction null
            }
            if (hideMessage(player)) {
                logger.debug { "Player ${player.name} is vanished." }
                return@transaction null
            }

            elixirPlayer.message()
        }
    }

    // TODO -> CMI Hook in Minix
    private fun hideMessage(player: Player) = when {
        player.gameMode == GameMode.SPECTATOR -> true
        player.isInvisible -> true
        runCatching { CMI.getInstance().playerManager.getUser(player)?.isVanished == true }.getOrDefault(false) -> true
        else -> false
    }
}

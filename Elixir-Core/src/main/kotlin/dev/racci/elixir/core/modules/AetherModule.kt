package dev.racci.elixir.core.modules

import dev.racci.elixir.core.constants.ElixirPermission
import dev.racci.elixir.core.data.ElixirConfig
import dev.racci.minix.api.events.player.PlayerMoveXYZEvent
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.events
import dev.racci.minix.api.extensions.hasPermissionOrStar
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object AetherModule : ModuleActor<ElixirConfig.Modules.Aether>() {
    override suspend fun load() {
        event(EventPriority.HIGHEST, true, block = ::handleVoid)

        events(
            PlayerJoinEvent::class,
            PlayerQuitEvent::class,
            PlayerChangedWorldEvent::class,
            priority = EventPriority.MONITOR,
            ignoreCancelled = true,
            block = ::handlePotions
        )

        events(
            BlockBreakEvent::class,
            BlockPlaceEvent::class,
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true,
            block = ::handleBlockMutate
        )
    }

    private fun <T : Event> handlePotions(event: T) {
        val config = this.getConfig()

        fun addPotions(player: Player) {
            val jumpBoost = config.jumpBoost
            val slowFalling = config.slowFalling

            if (jumpBoost > 0) {
                player.addPotionEffect(PotionEffect(PotionEffectType.JUMP, Int.MAX_VALUE, jumpBoost - 1, true, false, false))
            }

            if (slowFalling) {
                player.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, Int.MAX_VALUE, 0, true, false, false))
            }
        }

        fun removePotions(player: Player) {
            if (config.jumpBoost > 0) player.removePotionEffect(PotionEffectType.JUMP)
            if (config.slowFalling) player.removePotionEffect(PotionEffectType.SLOW_FALLING)
        }

        when {
            event is PlayerChangedWorldEvent -> {
                if (event.from.isAether()) {
                    removePotions(event.player)
                } else addPotions(event.player)
            }
            event is PlayerQuitEvent && event.player.world.isAether() -> removePotions(event.player)
            event is PlayerJoinEvent && event.player.world.isAether() -> addPotions(event.player)
        }
    }

    // TODO -> Intersection types
    private fun <T : Cancellable> handleBlockMutate(event: T) {
        val player = when (event) {
            is BlockBreakEvent -> event.player
            is BlockPlaceEvent -> event.player
            else -> return
        }

        val config = this.getConfig()

        if (!config.preventBlockMutate) return
        if (!player.world.isAether()) return
        if (player.hasPermissionOrStar(ElixirPermission.AEHTER_MUTATE.permissionString)) return

        event.cancel()
    }

    private fun handleVoid(event: PlayerMoveXYZEvent) {
        val config = this.getConfig()
        if (config.preventVoid == -1) return
        if (!event.player.world.isAether()) return

        if (event.player.location.y > config.preventVoid) return
        val teleportLocation = config.portalCords?.asBukkitLocation(event.player.world) ?: event.player.world.spawnLocation

        sync {
            event.player.teleport(teleportLocation)
        }
    }

    private fun World.isAether() = this.name.lowercase() == getConfig().worldName.lowercase()
}

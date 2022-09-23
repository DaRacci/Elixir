package dev.racci.elixir.core.services

import com.Zrips.CMI.CMI
import com.destroystokyo.paper.MaterialTags
import com.destroystokyo.paper.event.block.BeaconEffectEvent
import dev.racci.elixir.core.Elixir
import dev.racci.elixir.core.constants.ElixirPermission
import dev.racci.elixir.core.data.ElixirConfig
import dev.racci.elixir.core.data.ElixirLang
import dev.racci.elixir.core.data.ElixirPlayer
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.ticks
import dev.racci.minix.api.services.DataService
import dev.racci.minix.api.services.DataService.Companion.inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import org.bukkit.Effect
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftItem
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.potion.PotionEffect
import org.jetbrains.exposed.sql.transactions.transaction

@MappedExtension(Elixir::class, "Listener Service", [DataService::class])
class ListenerService(override val plugin: Elixir) : Extension<Elixir>() {
    private val elixirConfig by DataService.inject().inject<ElixirConfig>()
    private val elixirLang by DataService.inject().inject<ElixirLang>()

    override suspend fun handleEnable() {
        torchFireModule()
        drownConcreteModule()
        enhanceBeaconModule()
        joinLeaveMessageModule()

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
                transaction(getKoin().getProperty(Elixir.KOIN_DATABASE)) {
                    ElixirPlayer.findById(uuid) ?: ElixirPlayer.new(uuid) {}
                }
            } catch (e: Exception) {
                logger.error(e) { "Error while fetching player from database: ${e.message}" }
                this.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text("An error occurred while fetching ElixirPlayer profile. Please try again later.").color(NamedTextColor.RED))
            }
        }
    }

    private fun torchFireModule() {
        if (!elixirConfig.modules.torchFire.enabled) return

        val burnTicks = elixirConfig.modules.torchFire.burnTicks
        val torches by lazy {
            persistentListOf(
                Material.SOUL_TORCH,
                Material.TORCH
            )
        }
        event<EntityDamageByEntityEvent> {
            val attacker = damager as? LivingEntity
            val target = entity as? LivingEntity
            if (attacker == null || target == null || target.isDead || attacker.equipment?.getItem(attacker.handRaised)?.type !in torches) return@event
            if (target.fireTicks > burnTicks) return@event
            target.fireTicks = burnTicks
        }
    }

    private suspend fun drownConcreteModule() {
        if (!elixirConfig.modules.drownConcrete.enabled) return

        event<PlayerDropItemEvent>(priority = EventPriority.HIGH, ignoreCancelled = true, forceAsync = true) {
            if (!MaterialTags.CONCRETE_POWDER.isTagged(itemDrop.itemStack)) return@event

            var lastLocation = itemDrop.location
            delay(1.ticks)

            while (itemDrop.location != lastLocation) {
                lastLocation = itemDrop.location
                if (itemDrop.isInWater) {
                    val concreteType = itemDrop.itemStack.type.name.split('_')
                        .takeWhile { str -> str != "POWDER" }.joinToString("_")
                    itemDrop.itemStack.type = Material.valueOf(concreteType)
                    val nmsItem = (itemDrop as CraftItem).handle
                    val metadataPacket = ClientboundSetEntityDataPacket(itemDrop.entityId, nmsItem.entityData, true)
                    nmsItem.tracker?.serverEntity?.trackedPlayers?.forEach { it.send(metadataPacket) }
                    player.playSound(
                        Sound.sound(
                            Key.key("block.lava.extinguish"),
                            Sound.Source.NEUTRAL,
                            0.7f,
                            0.8f
                        ),
                        itemDrop.location.x,
                        itemDrop.location.y,
                        itemDrop.location.z
                    )
                    player.playEffect(itemDrop.location, Effect.COPPER_WAX_ON, null)
                    break
                }
                delay(2.ticks)
            }
        }
    }

    private suspend fun enhanceBeaconModule() {
        if (!elixirConfig.modules.enhanceBeacons.enabled) return

        if (elixirConfig.modules.enhanceBeacons.removeParticles) {
            event<BeaconEffectEvent>(EventPriority.HIGHEST, true) {
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

    private suspend fun joinLeaveMessageModule() {
        event<PlayerJoinEvent>(EventPriority.MONITOR, true) {
            this.joinMessage(message(player) { this.joinMessage ?: elixirLang.defaultJoinMessage["player" to { player.displayName() }] })
        }

        event<PlayerQuitEvent>(EventPriority.MONITOR, true) {
            this.quitMessage(message(player) { this.leaveMessage ?: elixirLang.defaultLeaveMessage["player" to { player.displayName() }] })
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

        return ElixirPlayer.transactionFuture {
            val elixirPlayer = ElixirPlayer[player.uniqueId]
            if (elixirPlayer.disableConnectionMessages) {
                logger.debug { "Player ${player.name} has disabled connection messages." }
                return@transactionFuture null
            }
            if (hideMessage(player)) {
                logger.debug { "Player ${player.name} is vanished." }
                return@transactionFuture null
            }

            elixirPlayer.message()
        }.get()
    }

    // TODO -> CMI Hook in Minix
    private fun hideMessage(player: Player) = when {
        player.gameMode == GameMode.SPECTATOR -> true
        player.isInvisible -> true
        runCatching { CMI.getInstance().playerManager.getUser(player)?.isVanished == true }.getOrDefault(false) -> true
        else -> false
    }
}

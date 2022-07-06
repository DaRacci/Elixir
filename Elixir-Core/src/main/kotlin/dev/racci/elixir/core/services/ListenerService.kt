package dev.racci.elixir.core.services

import com.destroystokyo.paper.MaterialTags
import dev.racci.elixir.core.Elixir
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.ticks
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import org.bukkit.Effect
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftItem
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.koin.core.component.inject

@MappedExtension(Elixir::class, "Listener Service", [StorageService::class, HookService::class])
class ListenerService(override val plugin: Elixir) : Extension<Elixir>() {
    private val hookService by inject<HookService>()
    private val storageService by inject<StorageService>()

    override suspend fun handleEnable() {
        if (storageService["modules.drownConcrete.enabled"]) {
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

        event<BlockFadeEvent>(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
        ) {
            if (!block.type.name.contains("CORAL") || !newState.type.name.startsWith("DEAD")) return@event
            hookService.getHook<HookService.LandsHook>()?.let { hook ->
                val manager = hook.manager ?: return@event
                val land = manager.getLand(block.location) ?: return@event
                val area = land.getArea(block.location) ?: return@event
                if (area.hasFlag(hook.coralDecayFlag)) cancel()
            }
        }

        if (storageService["modules.torchFire.enabled"]) {
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
                val ticks: Int = storageService["modules.torchFire.ticks"]
                if (target.fireTicks > ticks) return@event
                target.fireTicks = ticks
            }
        }
    }
}

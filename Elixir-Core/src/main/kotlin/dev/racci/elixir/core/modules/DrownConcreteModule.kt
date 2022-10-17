package dev.racci.elixir.core.modules

import com.destroystokyo.paper.MaterialTags
import dev.racci.elixir.core.data.ElixirConfig
import dev.racci.minix.api.extensions.collections.findKProperty
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.reflection.accessGet
import dev.racci.minix.api.extensions.ticks
import kotlinx.coroutines.delay
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.server.level.ChunkMap.TrackedEntity
import net.minecraft.server.level.ServerEntity
import org.bukkit.Effect
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftItem
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerDropItemEvent
import kotlin.reflect.full.declaredMemberProperties

object DrownConcreteModule : ModuleActor<ElixirConfig.Modules.DrownConcrete>() {

    override suspend fun load() {
        event(EventPriority.HIGH, ignoreCancelled = true, forceAsync = true, ::handleDrown)
    }

    private suspend inline fun handleDrown(event: PlayerDropItemEvent) {
        if (!MaterialTags.CONCRETE_POWDER.isTagged(event.itemDrop.itemStack)) return

        var lastLocation = event.itemDrop.location
        delay(1.ticks)

        while (event.itemDrop.location != lastLocation) {
            lastLocation = event.itemDrop.location
            if (event.itemDrop.isInWater) {
                val concreteType = event.itemDrop.itemStack.type.name.split('_')
                    .takeWhile { str -> str != "POWDER" }.joinToString("_")
                event.itemDrop.itemStack.type = Material.valueOf(concreteType)
                val nmsItem = (event.itemDrop as CraftItem).handle
                val metadataPacket = ClientboundSetEntityDataPacket(event.itemDrop.entityId, nmsItem.entityData, true)
                if (nmsItem.tracker != null) {
                    TrackedEntity::class.declaredMemberProperties.findKProperty<ServerEntity>("serverEntity")
                        .map { it.accessGet(nmsItem.tracker!!) }
                        .map(ServerEntity::trackedPlayers)
                        .orNull()?.forEach { player -> player.send(metadataPacket) }
                }
                event.player.playSound(
                    Sound.sound(
                        Key.key("block.lava.extinguish"),
                        Sound.Source.NEUTRAL,
                        0.7f,
                        0.8f
                    ),
                    event.itemDrop.location.x,
                    event.itemDrop.location.y,
                    event.itemDrop.location.z
                )
                event.player.playEffect(event.itemDrop.location, Effect.COPPER_WAX_ON, null)
                break
            }
            delay(2.ticks)
        }
    }
}

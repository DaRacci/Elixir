package dev.racci.elixir.core.modules

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import dev.racci.elixir.core.data.ElixirConfig
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.events
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.extensions.toNamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

public object HubModule : ModuleActor<ElixirConfig.Modules.Hub>() {
    private val hubKey = "elixir:hub_modifier".toNamespacedKey()

    override suspend fun load() {
        events(
            PlayerJoinEvent::class,
            PlayerPostRespawnEvent::class,
            priority = EventPriority.HIGHEST
        ) {
            if (!withinHub(this.player)) return@events
            cleanPlayer(this.player)
            contaminatePlayer(this.player)
        }

        event<PlayerChangedWorldEvent>(EventPriority.HIGHEST, true) {
            val worlds = getConfig().worlds
            val fromIsHub = this.from.name in worlds
            val toIsHub = this.player.world.name in worlds

            when {
                fromIsHub && toIsHub -> return@event
                fromIsHub -> cleanPlayer(this.player)
                toIsHub -> contaminatePlayer(this.player)
            }
        }

        event<EntityPotionEffectEvent>(EventPriority.HIGHEST, true) {
            if (entityType !== EntityType.PLAYER) return@event
            if (!withinHub(this.entity.castOrThrow())) return@event
            if (oldEffect == null && newEffect?.key == hubKey) return@event
            if (action == EntityPotionEffectEvent.Action.REMOVED) return@event

            if (oldEffect?.key == hubKey && newEffect?.key != hubKey) {
                logger.debug { "Cancelling potion effect change for protected potion." }
                return@event cancel()
            }
        }

        event<FoodLevelChangeEvent>(EventPriority.HIGHEST, true) {
            if (this.entityType !== EntityType.PLAYER || !withinHub(this.entity.castOrThrow())) return@event
            cancel()
        }
    }

    private fun withinHub(player: Player): Boolean = player.world.name in getConfig().worlds

    private fun cleanPlayer(player: Player) {
        player.activePotionEffects.map(PotionEffect::getType).forEach(player::removePotionEffect)
        Attribute.values().mapNotNull(player::getAttribute).forEach { instance -> instance.modifiers.forEach(instance::removeModifier) }
    }

    private fun contaminatePlayer(player: Player) {
        val config = getConfig()

        player.foodLevel = 20
        player.saturation = 20f
        player.exhaustion = 0f
        player.health = getAttribute(player, Attribute.GENERIC_MAX_HEALTH).value
        player.fireTicks = 0
        player.fallDistance = 0f

        with(getAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED)) {
            this.addModifier(AttributeModifier(hubKey.asString(), config.speedMultiplier - 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1))
        }

        player.addPotionEffect(PotionEffect(PotionEffectType.JUMP, Int.MAX_VALUE, config.jumpLevel - 1, true, false, false, hubKey))
    }

    private fun getAttribute(
        player: Player,
        attribute: Attribute
    ): AttributeInstance = player.getAttribute(attribute) ?: player.registerAttribute(attribute).let { player.getAttribute(attribute)!! }
}

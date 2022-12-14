package dev.racci.elixir.core.modules

import dev.racci.elixir.core.Elixir
import dev.racci.elixir.core.data.ElixirConfig
import dev.racci.minix.api.extensions.KListener
import dev.racci.minix.api.extensions.event
import kotlinx.collections.immutable.persistentListOf
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent

public object TorchFireModule : ModuleActor<ElixirConfig.Modules.TorchFire>() {
    private val torches by lazy {
        persistentListOf(
            Material.SOUL_TORCH,
            Material.TORCH
        )
    }

    override suspend fun registerListeners(listener: KListener<Elixir>) {
        listener.event<EntityDamageByEntityEvent>(EventPriority.MONITOR, true) {
            val config = getConfig()
            val burnTicks = config.burnTicks
            val attacker = damager as? LivingEntity
            val target = entity as? LivingEntity

            if (attacker == null || target == null || target.isDead || attacker.equipment?.getItem(attacker.handRaised)?.type !in torches) return@event
            if (target.fireTicks > burnTicks) return@event

            target.fireTicks = burnTicks
        }
    }
}

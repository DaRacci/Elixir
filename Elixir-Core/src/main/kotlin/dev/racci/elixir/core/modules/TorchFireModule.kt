package dev.racci.elixir.core.modules

import dev.racci.elixir.core.data.ElixirConfig
import dev.racci.minix.api.extensions.event
import kotlinx.collections.immutable.persistentListOf
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent

object TorchFireModule : ModuleActor<ElixirConfig.Modules.TorchFire>() {
    private val torches by lazy {
        persistentListOf(
            Material.SOUL_TORCH,
            Material.TORCH
        )
    }

    override suspend fun load() {
        event(EventPriority.HIGH, true, block = ::handleAttack)
    }

    private fun handleAttack(event: EntityDamageByEntityEvent) {
        val config = this.getConfig()
        val burnTicks = config.burnTicks
        val attacker = event.damager as? LivingEntity
        val target = event.entity as? LivingEntity

        if (attacker == null || target == null || target.isDead || attacker.equipment?.getItem(attacker.handRaised)?.type !in torches) return
        if (target.fireTicks > burnTicks) return

        target.fireTicks = burnTicks
    }
}

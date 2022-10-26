package dev.racci.elixir.core.modules

import dev.racci.elixir.core.data.ElixirConfig
import dev.racci.elixir.core.data.ElixirLang
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.utils.adventure.PartialComponent.Companion.message
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.koin.core.component.get

object EggTrackerModule : ModuleActor<ElixirConfig.Modules.EggTracker>() {
    override suspend fun load() {
        event<PlayerDropItemEvent> {
            if (this.itemDrop.itemStack.type != Material.DRAGON_EGG) return@event

            this.cancel()
            get<ElixirLang>().eggTracker.cannotDrop message this.player
        }

        event<InventoryMoveItemEvent>() {
            if (this.item.type != Material.DRAGON_EGG) return@event
            this.cancel()
        }
    }

    // Command for listing who has the egg.
    // Cannot remove from inventory and will drop on death.
    // If an egg despawns the next dragon killed drops an egg
}

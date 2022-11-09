package dev.racci.elixir.core.modules

import de.codingair.tradesystem.spigot.TradeSystem
import dev.racci.elixir.core.data.ElixirConfig
import dev.racci.elixir.core.data.ElixirLang
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.message
import dev.racci.minix.api.extensions.server
import dev.racci.minix.api.extensions.worlds
import dev.racci.minix.api.utils.adventure.PartialComponent.Companion.message
import io.papermc.paper.event.block.DragonEggFormEvent
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.inventory.ItemStack
import org.koin.core.component.get

// TODO -> Command for listing who has the egg.
// TODO -> Cannot remove from inventory and will drop on death.
// TODO -> If an egg despawns the next dragon killed drops an egg
object EggTrackerModule : ModuleActor<ElixirConfig.Modules.EggTracker>() {
    private var dropEgg: Boolean = false

    override suspend fun load() {
        event<PlayerDropItemEvent>(EventPriority.LOWEST) {
            if (!isDragonEgg(this.itemDrop.itemStack)) return@event

            cancel()
            get<ElixirLang>().eggTracker.cannotDrop message this.player
        }

        event<InventoryClickEvent>(EventPriority.LOWEST) {
            if (!isDragonEgg(this.currentItem, this.cursor)) return@event
            if (isTrade(this.whoClicked)) return@event

            when {
                this.action == InventoryAction.MOVE_TO_OTHER_INVENTORY && click.isShiftClick -> cancel()
                this.action.name.startsWith("PLACE") && this.clickedInventory != this.whoClicked.inventory -> cancel()
            }
        }

        event<InventoryDragEvent>(EventPriority.LOWEST) {
            if (!isDragonEgg(this.cursor, this.oldCursor)) return@event
            if (isTrade(this.whoClicked)) return@event
            if (this.rawSlots.any { it <= this.inventory.size }) cancel() // Blocks any slots which is within the open inventory since the players raw slots are above the inventory size.
        }

        event<BlockPlaceEvent>(EventPriority.LOWEST) {
            if (isDragonEgg(this.itemInHand)) cancel()
        }

        event<ItemDespawnEvent>(EventPriority.LOWEST) {
            if (!isDragonEgg(this.entity.itemStack)) return@event
            get<ElixirLang>().eggTracker.despawned.get() message server

            val end = worlds.find { it.environment == World.Environment.THE_END } ?: return@event
            end.enderDragonBattle!!.initiateRespawn()
        }

        event<DragonEggFormEvent>(EventPriority.HIGHEST) {
            if (dropEgg) {
                isCancelled = false
                dropEgg = false
            } else cancel()
        }
    }

    private fun isTrade(humanEntity: HumanEntity): Boolean {
        if (humanEntity !is Player) return false

        return runCatching { TradeSystem.man().isTrading(humanEntity) }.getOrDefault(false)
    }

    private fun isDragonEgg(vararg item: ItemStack?): Boolean {
        return item.any { it?.type == Material.DRAGON_EGG }
    }
}

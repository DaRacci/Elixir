package dev.racci.elixir.core.modules

import dev.racci.elixir.core.data.ElixirConfig
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.scheduler
import dev.racci.minix.api.extensions.ticks
import org.bukkit.block.Bed
import org.bukkit.event.Event
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerInteractEvent

public object SleepWithYoHomiesModule : ModuleActor<ElixirConfig.Modules.SleepWithYoHomies>() {
    override suspend fun load() {
        event<PlayerInteractEvent>() {
            if (this.action == Action.RIGHT_CLICK_BLOCK && this.clickedBlock?.blockData is Bed) {
                scheduler {
                    this.player.sleep(this.clickedBlock!!.location, true)
                }.runTaskLater(plugin, 4.ticks)
            }
        }

        event<PlayerBedEnterEvent>() { this.setUseBed(Event.Result.ALLOW) }
    }
}

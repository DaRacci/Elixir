package dev.racci.elixir.api.events

import dev.racci.elixir.api.data.challenge.ResourceChallengeType
import dev.racci.minix.api.events.CompanionEventHandler
import dev.racci.minix.api.events.KEvent
import org.bukkit.event.HandlerList
import kotlin.time.Duration

public data class ResourceChallengeEvent(
    public val challenge: ResourceChallengeType,
    public val amount: Int,
    public val duration: Duration
) : KEvent(true) {
    public companion object : CompanionEventHandler() {
        @JvmStatic
        public override fun getHandlerList(): HandlerList = super.getHandlerList()
    }
}

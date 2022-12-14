package dev.racci.elixir.api.events

import dev.racci.elixir.api.data.challenge.Challenge
import dev.racci.minix.api.events.CompanionEventHandler
import org.bukkit.event.HandlerList

public class ChallengeEndEvent(
    challenge: Challenge
) : ChallengeEvent(challenge) {
    public companion object : CompanionEventHandler() {
        @JvmStatic override fun getHandlerList(): HandlerList = super.getHandlerList()
    }
}

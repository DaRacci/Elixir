package dev.racci.elixir.api.events

import dev.racci.elixir.api.data.challenge.Challenge
import dev.racci.minix.api.events.KEvent

public sealed class ChallengeEvent protected constructor(
    public val challenge: Challenge
) : KEvent(true)

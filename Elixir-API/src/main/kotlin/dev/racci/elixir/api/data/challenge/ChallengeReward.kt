package dev.racci.elixir.api.data.challenge

import dev.racci.elixir.api.data.ItemStackSerializer
import kotlinx.serialization.Serializable
import org.bukkit.inventory.ItemStack

@Serializable
public data class ChallengeReward(
    public val items: List<@Serializable(with = ItemStackSerializer::class) ItemStack>,
    public val experience: Long,
    public val money: Long
)

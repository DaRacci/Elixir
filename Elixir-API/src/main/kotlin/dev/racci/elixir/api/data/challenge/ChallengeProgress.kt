package dev.racci.elixir.api.data.challenge

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.fren_gor.ultimateAdvancementAPI.UltimateAdvancementAPI
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType
import dev.racci.elixir.api.data.PlayerRelatedEntity
import dev.racci.elixir.api.data.PlayerRelatedEntityClass
import dev.racci.elixir.api.data.PlayerRelationTable
import dev.racci.minix.api.extensions.onlinePlayer
import dev.racci.minix.api.utils.getKoin
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.bukkit.Material
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.exposed.dao.ColumnWithTransform
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import java.util.UUID

public class ChallengeProgress(uuid: EntityID<UUID>) : PlayerRelatedEntity(uuid) {
    public companion object : PlayerRelatedEntityClass<ChallengeProgress>(ChallengeProgression)

    public var challenge: Challenge by Challenge referencedOn ChallengeProgression.relational
    public var progress: Long by ChallengeProgression.progress; private set
    public var claimedRewards: Boolean by ChallengeProgression.claimedRewards; private set
    public var completedAt: Option<Instant> by ChallengeProgression.completedAt; private set

    public val completed: Boolean
        get() = completedAt is Some

    public fun claimRewards() {
        if (completed) return

        claimedRewards = true
        // TODO: Add reward system
    }

    public fun incrementProgress(amount: Long) {
        if (challenge.isExpired) return

        progress += amount

        if (challenge.requiredAmount <= progress) {
            completedAt = Some(Clock.System.now())
            getKoin().get<UltimateAdvancementAPI>().displayCustomToast(
                onlinePlayer(id.value) ?: return,
                AdvancementDisplay(
                    Material.AMETHYST_SHARD,
                    "Challenge Completed",
                    AdvancementFrameType.CHALLENGE,
                    true,
                    false,
                    0f,
                    0f,
                    "Challenge Completed blah blah",
                    "blah blah",
                    "blah blah"
                )
            )
        }
    }

    public fun decrementProgress(amount: Long) {
        if (challenge.isExpired) return

        progress -= amount

        if (challenge.requiredAmount > progress) {
            completedAt = None
        }
    }

    @ApiStatus.Internal
    public object ChallengeProgression : PlayerRelationTable<Int>(Challenge.Challenges, "challenge_players") {
        public val progress: Column<Long> = long("progress").default(0)

        public val claimedRewards: Column<Boolean> = bool("rewards_claimed").default(false)

        public val completedAt: ColumnWithTransform<Long?, Option<Instant>> = long("completed_at")
            .nullable()
            .default(null)
            .transform({ option -> option.map(Instant::toEpochMilliseconds).orNull() }, { epoch -> Option.fromNullable(epoch?.let(Instant::fromEpochMilliseconds)) })
    }
}

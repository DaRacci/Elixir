package dev.racci.elixir.api.data.challenge

import arrow.core.Option
import arrow.core.Some
import dev.racci.elixir.api.data.ElixirPlayer
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.exposed.dao.ColumnWithTransform
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import java.util.UUID

public class ChallengeProgress private constructor(uuid: EntityID<UUID>) : UUIDEntity(uuid) {
    public companion object : UUIDEntityClass<ChallengeProgress>(ChallengeProgression)

    public val player: ElixirPlayer by ElixirPlayer referencedOn ChallengeProgression.player
    public val challenge: Challenge by Challenge referencedOn ChallengeProgression.challenge
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
            // TODO: Notification of completion via a thingy in the top right corner that looks like a notification
        }
    }

    @ApiStatus.Internal
    public object ChallengeProgression : UUIDTable("challenge_players") {
        public val challenge: Column<EntityID<UUID>> = reference("challenge", Challenge.Challenges)

        public val player: Column<EntityID<UUID>> = reference("player", ElixirPlayer.ElixirPlayers)

        public val progress: Column<Long> = long("progress")

        public val claimedRewards: Column<Boolean> = bool("rewards_claimed")

        public val completedAt: ColumnWithTransform<Long?, Option<Instant>> = long("completed_at").nullable()
            .transform({ option -> option.map(Instant::toEpochMilliseconds).orNull() }, { epoch -> Option.fromNullable(epoch?.let(Instant::fromEpochMilliseconds)) })
    }
}

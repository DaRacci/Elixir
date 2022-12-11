package dev.racci.elixir.api.data.challenge

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.exposed.dao.ColumnWithTransform
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.kotlin.datetime.duration
import java.util.UUID
import kotlin.time.Duration

public class Challenge private constructor(uuid: EntityID<UUID>) : UUIDEntity(uuid) {
    public companion object : UUIDEntityClass<Challenge>(Challenges)

    public val challenge: ResourceChallengeType by Challenges.challengeType
    public val requiredAmount: Long by Challenges.requiredAmount
    public val duration: Duration by Challenges.duration
    public val start: Instant by Challenges.start

    public val isExpired: Boolean
        get() = Clock.System.now() >= (start + duration)

    public val participants: SizedIterable<ChallengeProgress> by ChallengeProgress referrersOn ChallengeProgress.ChallengeProgression.challenge

    @ApiStatus.Internal
    public object Challenges : UUIDTable("challenge") {
        public val challengeType: Column<ResourceChallengeType> = enumeration("challenge_type", ResourceChallengeType::class)

        public val requiredAmount: Column<Long> = long("required_amount")

        public val duration: Column<Duration> = duration("duration")

        public val start: ColumnWithTransform<Long, Instant> = long("start")
            .check { it lessEq Clock.System.now().toEpochMilliseconds() }
            .transform(Instant::toEpochMilliseconds, Instant::fromEpochMilliseconds)
    }
}

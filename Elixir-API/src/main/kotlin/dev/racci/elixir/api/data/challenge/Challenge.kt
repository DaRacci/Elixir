package dev.racci.elixir.api.data.challenge

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import dev.racci.minix.api.extensions.reflection.castOrThrow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.exposed.dao.ColumnWithTransform
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.kotlin.datetime.duration
import kotlin.time.Duration

public class Challenge(id: EntityID<Int>) : IntEntity(id) {
    public companion object : IntEntityClass<Challenge>(Challenges)

    public var challenge: ResourceChallengeType by Challenges.challengeType
    public var requiredType: Option<Challenges.ResourceType> by Challenges.requiredType
    public var requiredAmount: Long by Challenges.requiredAmount
    public var duration: Duration by Challenges.duration
    public var start: Instant by Challenges.start
    // public val reward: ChallengeReward

    public val remainingTime: Duration
        get() = duration - (Clock.System.now() - start)

    public val isExpired: Boolean
        get() = Clock.System.now() >= (start + duration)

    @ApiStatus.Internal
    public object Challenges : IntIdTable("challenge") {
        public val challengeType: Column<ResourceChallengeType> = enumeration("challenge_type", ResourceChallengeType::class)

        public val requiredType: ColumnWithTransform<String?, Option<ResourceType>> = text("required_type")
            .nullable()
            .default(null)
            .transform(
                { type ->
                    if (type !is Some) {
                        null
                    } else buildString {
                        append(type.value.type::class.qualifiedName)
                        append(':')
                        append(type.value.type.name)
                    }
                },
                { raw ->
                    if (raw == null) return@transform None

                    val (type, name) = raw.split(':')
                    val kClass = Class.forName(type).kotlin
                    val value = kClass.java.enumConstants.castOrThrow<Array<out Enum<*>>>().first { it.name == name }
                    Some(ResourceType(value))
                }
            )

        public data class ResourceType(val type: Enum<*>) {
            public fun matches(material: Material): Boolean = when (type) {
                is Material -> type == material
                else -> false
            }

            public fun matches(entity: EntityType): Boolean = when (type) {
                is EntityType -> type == entity
                else -> false
            }
        }

        public val requiredAmount: Column<Long> = long("required_amount")

        public val duration: Column<Duration> = duration("duration")

        public val start: ColumnWithTransform<Long, Instant> = long("start")
//            .clientDefault(Clock.System.now()::toEpochMilliseconds)
//            .check { it lessEq Clock.System.now().toEpochMilliseconds() }
            .transform(Instant::toEpochMilliseconds, Instant::fromEpochMilliseconds)
    }
}

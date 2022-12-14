package dev.racci.elixir.api.data.challenge

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import dev.racci.elixir.api.data.challenge.Challenge.Challenges.ResourceType
import dev.racci.minix.api.extensions.isOre
import org.bukkit.Material
import org.bukkit.entity.Boss
import org.bukkit.entity.EntityType
import kotlin.random.Random

public enum class ResourceChallengeType {
    COLLECTION {
        override fun generate(): Option<ResourceType> = Some(ResourceType(naturalBlocks.random()))
        override fun generateAmount(resourceType: ResourceType): Long {
            val material = resourceType.type as Material
            return when {
                material.isOre -> Random.nextLong(50, 250) // TODO: Calculate amount based on rareness of ore
                else -> Random.nextLong(300, 1000) // TODO: Calculate amount based on block hardness
            }
        }
    },
    KILLS {
        override fun generate(): Option<ResourceType> = Some(ResourceType(livingEntities.random()))
        override fun generateAmount(resourceType: ResourceType): Long {
            val entityType = resourceType.type as EntityType
            return Random.nextLong(10, 50)
        }
    },
    DAMAGE {
        override fun generate(): Option<ResourceType> = None
        override fun generateAmount(resourceType: ResourceType): Long {
            return Random.nextLong(3000, 10000)
        }
    };

    public abstract fun generate(): Option<ResourceType>

    public abstract fun generateAmount(resourceType: ResourceType): Long

    private companion object {
        private val naturalBlocks by lazy {
            Material.values().asSequence()
                .filter { it.isBlock }
                .filter { true /* TODO: Define naturally spawning blocks */ }
                .toList()
        }

        private val livingEntities by lazy {
            EntityType.values().asSequence()
                .filter { it.isAlive }
                .filterNot { it.entityClass == null }
                .filterNot { it.entityClass!!.isAssignableFrom(Boss::class.java) }
                .toList()
        }
    }
}

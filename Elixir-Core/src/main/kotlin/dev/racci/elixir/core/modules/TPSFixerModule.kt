package dev.racci.elixir.core.modules

import dev.racci.elixir.core.data.ElixirConfig
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.scheduler
import dev.racci.minix.api.extensions.worlds
import dev.racci.minix.api.scheduler.CoroutineScheduler
import kotlinx.coroutines.Deferred
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Tameable
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.inventory.Merchant
import kotlin.math.min
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object TPSFixerModule : ModuleActor<ElixirConfig.Modules.TPSFixer>() {
    private var spawnRate = 1.0f
    private var spawnRateTask = -1

    override suspend fun load() {
        this.startTPSPoller()

        event<EntitySpawnEvent> {
            if (Random.nextFloat() > spawnRate) return@event cancel()

            val keepBelow = getConfig().mutateSpawnRate[entity.type] ?: return@event
            if (Random.nextFloat() > keepBelow) return@event

            cancel()
        }
    }

    override suspend fun close() {
        CoroutineScheduler.shutdownTask(spawnRateTask)
    }

    private suspend fun startTPSPoller() {
        if (spawnRateTask != -1 && CoroutineScheduler.shutdownTask(spawnRateTask)) {
            logger.warn { "Cancelled previous TPS poller task" }
        }

        spawnRateTask = scheduler {
            val tps = min(Bukkit.getTPS()[0], Bukkit.getTPS()[1])
            val lastTps = Bukkit.getTPS()[0]
            val allEntities = deferredSync { worlds.flatMap(World::getEntities) } // We might need this twice or not at all so let's defer it.

            this.maybeMutateSpawnRate(tps)

            this.maybeCullSpawnerMobs(tps, allEntities)
            this.maybeCullAllMobs(lastTps, allEntities)
        }.runAsyncTaskTimer(plugin, Duration.ZERO, 10.seconds).taskID
    }

    private suspend fun maybeCullSpawnerMobs(
        curTPS: Double,
        allEntities: Deferred<List<Entity>>
    ) {
        val threshold = getConfig().spawnerTPSThreshold
        if (threshold == -1.0 || curTPS > threshold) return

        logger.info { "TPS is below threshold - Killing spawner mobs!" }
        for (entity in allEntities.await()) {
            if (entity.fromMobSpawner()) {
                entity.remove()
            }
        }
    }

    private suspend fun maybeCullAllMobs(
        lastTPS: Double,
        allEntities: Deferred<List<Entity>>
    ) {
        if (lastTPS >= 18) return

        logger.info { "Most recent TPS is $lastTPS; killing 25% of mobs!" }
        for (entity in allEntities.await()) {
            if (Random.nextFloat() > 0.25f) continue
            if (entity.isValuable()) continue
        }
    }

    private fun maybeMutateSpawnRate(curTPS: Double) {
        getConfig().spawnTPSMultiplier.forEach { (targetTPS, multiplier) ->
            if (curTPS > targetTPS) return@forEach
            if (targetTPS == -1.0) {
                spawnRate = multiplier
                return@forEach
            }

            spawnRate = multiplier
        }

        if (spawnRate != 1f) {
            logger.info { "TPS is $curTPS; spawn rate = $spawnRate" }
        }
    }

    private fun Entity.isValuable() = when (this) {
        is Tameable -> true
        is ItemFrame -> true
        is ArmorStand -> true
        is Merchant -> true
        is LivingEntity -> this.customName() != null
        else -> false
    }
}

package dev.racci.elixir.core.modules

import cloud.commandframework.arguments.standard.IntegerArgument
import cloud.commandframework.arguments.standard.LongArgument
import cloud.commandframework.kotlin.extension.buildAndRegister
import cloud.commandframework.minecraft.extras.RichDescription
import cloud.commandframework.paper.PaperCommandManager
import com.github.benmanes.caffeine.cache.Caffeine
import com.willfp.eco.core.events.EntityDeathByEntityEvent
import dev.racci.elixir.api.data.challenge.Challenge
import dev.racci.elixir.api.data.challenge.ChallengeProgress
import dev.racci.elixir.api.data.challenge.ChallengeProgress.ChallengeProgression
import dev.racci.elixir.api.data.challenge.ResourceChallengeType
import dev.racci.elixir.api.events.ChallengeEndEvent
import dev.racci.elixir.api.events.ChallengeStartEvent
import dev.racci.elixir.core.Elixir
import dev.racci.elixir.core.data.ElixirConfig
import dev.racci.elixir.core.data.ElixirLang
import dev.racci.elixir.core.extensions.playerFlag
import dev.racci.elixir.core.extensions.sub
import dev.racci.elixir.core.extensions.targetElseSender
import dev.racci.elixir.core.services.ElixirStorageService
import dev.racci.minix.api.extensions.KListener
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.message
import dev.racci.minix.api.extensions.msg
import dev.racci.minix.api.extensions.onlinePlayers
import dev.racci.minix.api.extensions.player
import dev.racci.minix.api.extensions.scheduler
import dev.racci.minix.api.extensions.server
import kotlinx.coroutines.flow.asFlow
import kotlinx.datetime.Clock
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.extra.kotlin.text
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.jetbrains.exposed.sql.and
import org.koin.core.component.get
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

// TODO: Leaderboard Placement based rewards
public object ChallengeModule : ExperimentalActor<ElixirConfig.Modules.ResourceChallenge>() {
    public lateinit var activeChallenge: Challenge
    private val bossBars = Caffeine.newBuilder().weakKeys()
        .removalListener<Player, BossBar> { player, value, _ -> player!!.hideBossBar(value!!) }
        .build<Player, BossBar> { player ->
            BossBar.bossBar(
                text {
                    Component.text(
                        buildString {
                            append(activeChallenge.challenge.name)
                            append(" | ")
                            append(activeChallenge.requiredAmount)
                            append(" - ")
                            append(activeChallenge.requiredType.map { it.type.name }.orNull() ?: "Any")
                            append(" | ")
                        }
                    ).also(::append)
                    append(Component.text(activeChallenge.remainingTime.toString(DurationUnit.MINUTES)))
                },
                0.0F,
                BossBar.Color.BLUE,
                BossBar.Overlay.PROGRESS
            ).also(player::showBossBar)
        }

    override suspend fun shouldLoad(): Boolean {
        return super.shouldLoad() && plugin.version.isPreRelease // TODO: Remove this when the module is ready
    }

    override suspend fun load() {
        ElixirStorageService.transaction {
            val existingChallenge = Challenge.all().lastOrNull()?.takeUnless(Challenge::isExpired)
            if (existingChallenge != null) {
                activeChallenge = existingChallenge
            } else startChallenge()
        }

        scheduler {
            if (activeChallenge.isExpired) {
                ElixirStorageService.transaction {
                    maybeEndChallenge()
                    maybeStartChallenge()
                }
            } else {
                val remaining = activeChallenge.remainingTime
                bossBars.asMap().forEach { (_, bossBar) ->
                    val children = bossBar.name().children()
                    val mutCopy = children.take(children.size - 1).toMutableList()
                    mutCopy.add(Component.text(remaining.toString(DurationUnit.MINUTES)))
                    bossBar.name(Component.join(JoinConfiguration.noSeparators(), mutCopy))
                }
            }
        }.runAsyncTaskTimer(plugin, Duration.ZERO, 250.milliseconds)

        // Sorted leaderboard for the active challenge
        // Leaderboard for all time challenges (maybe?)
        // Viewable challenge history
    }

    override suspend fun registerListeners(listener: KListener<Elixir>) {
        listener.event<PlayerJoinEvent>(EventPriority.MONITOR, true) {
            bossBars[player]
        }

        listener.event<BlockBreakEvent>(EventPriority.MONITOR, true) {
            if (activeChallenge.challenge != ResourceChallengeType.COLLECTION) return@event
            if (activeChallenge.requiredType.exists { !it.matches(block.type) }) return@event

            incrementParticipant(player, 1)
        }

        listener.event<EntityDamageByEntityEvent>(EventPriority.MONITOR, true) {
            val player = damager as? Player ?: return@event
            if (activeChallenge.challenge != ResourceChallengeType.DAMAGE) return@event
            if (activeChallenge.requiredType.exists { !it.matches(entity.type) }) return@event

            incrementParticipant(player, finalDamage.toLong())
        }

        listener.event<EntityDeathByEntityEvent>(EventPriority.MONITOR, true) {
            val player = killer as? Player ?: return@event
            if (activeChallenge.challenge != ResourceChallengeType.KILLS) return@event
            if (activeChallenge.requiredType.exists { !it.matches(victim.type) }) return@event

            incrementParticipant(player, 1)
        }
    }

    override suspend fun registerCommands(manager: PaperCommandManager<CommandSender>) {
        manager.buildAndRegister(
            "challenge",
            RichDescription.empty(),
            emptyArray()
        ) {
            registerCopy("leaderboard") {
                flag("page", arrayOf("p"), RichDescription.empty(), IntegerArgument.newBuilder<CommandSender?>("page").withMin(1).withMax(50))
                handler { ctx ->
                    val page = ctx.flags().get<Int>("page") ?: 1
                    val leaderboard = ElixirStorageService.transaction {
                        ChallengeProgress.find { ChallengeProgression.relational eq activeChallenge.id }
                            .sortedByDescending(ChallengeProgress::progress)
                            .mapIndexed { index, progress ->
                                val player = player(progress.id.value) ?: return@mapIndexed null
                                val progressComponent = Component.text(progress.progress)
                                val nameComponent = player.displayName()
                                val rankComponent = Component.text("#${index + 1}")
                                Component.join(
                                    JoinConfiguration.separator(Component.text(" | ")),
                                    rankComponent,
                                    nameComponent,
                                    progressComponent
                                )
                            }.filterNotNull().chunked(10)
                    }

                    if (page > leaderboard.size) {
                        ctx.sender.sendMessage("Invalid page, must be below ${leaderboard.size}") // TODO: Message
                        return@handler
                    }

                    MiniMessage.miniMessage().deserialize(
                        "Active challenge Leaderboard | Page <page> of <total>",
                        TagResolver.resolver("page", Tag.inserting(Component.text(page.toString()))),
                        TagResolver.resolver("total", Tag.inserting(Component.text(leaderboard.size.toString())))
                    ) message ctx.sender // TODO: Message

                    if (leaderboard.isEmpty()) {
                        ctx.sender.sendMessage("No progress yet!") // TODO: Message
                        return@handler
                    }

                    leaderboard[page - 1].forEach { component -> component message ctx.sender }
                }
            }
            registerCopy("cancel") {
                permission(modulePermission.sub("cancel"))
                handler { ElixirStorageService.transaction { activeChallenge.duration = Duration.ZERO } }
            }
            registerCopy("mutate") {
                permission(modulePermission.sub("mutate"))

                arrayOf(
                    copy("increment") { handler { ctx -> incrementParticipant(ctx.targetElseSender(), ctx.flags().get<Long>("amount")!!) } },
                    copy("decrement") { handler { ctx -> decrementParticipant(ctx.targetElseSender(), ctx.flags().get<Long>("amount")!!) } }
                ).forEach { copy ->
                    copy.playerFlag()
                    copy.flag("amount", arrayOf("a"), RichDescription.empty(), LongArgument.newBuilder<CommandSender>("amount").withMin(1))
                    copy.register()
                }
            }
            registerCopy("claim") {
                senderType<Player>()
                handler { ctx ->
                    val player = ctx.sender as Player
                    ElixirStorageService.transaction {
                        val progress = ChallengeProgress[player.uniqueId]
                        if (!progress.completed) {
                            ctx.sender.msg("You have not completed the challenge yet!") // TODO: Message
                            return@transaction
                        }

//                        val reward = activeChallenge.reward
                    }
                }
            }
        }
    }

    private suspend fun maybeStartChallenge() {
        if (::activeChallenge.isInitialized && !activeChallenge.isExpired) return
        this.startChallenge()
    }

    // TODO: If DiscordSRV is present create automated discord ping
    private suspend fun startChallenge() {
        activeChallenge = Challenge.new {
            this.challenge = ResourceChallengeType.values().random()
            this.requiredType = this.challenge.generate()
            this.requiredAmount = 10
            this.duration = durationFor(this.challenge)
            this.start = Clock.System.now()
        }.also { challenge ->
            ChallengeStartEvent(challenge).callEvent()

            Server broadcastMessage text {
                append(Component.text("A new challenge has started"))
                append(Component.text("The challenge type is ${challenge.challenge.name}"))
                append(Component.text("The required type is ${challenge.requiredType.map { it.type.name }.orNull() ?: "Any"}"))
                append(Component.text("The required amount is ${challenge.requiredAmount}"))
                append(Component.text("The duration is ${challenge.duration}"))
            }
        }

        bossBars.refreshAll(onlinePlayers)
    }

    private fun maybeEndChallenge() {
        if (!::activeChallenge.isInitialized || !activeChallenge.isExpired) return
        this.endChallenge()
    }

    private fun endChallenge() {
        if (!::activeChallenge.isInitialized || activeChallenge.isExpired) return

        ChallengeEndEvent(activeChallenge).callEvent()

        if (!activeChallenge.isExpired) { // We are forcefully ending the challenge early, so we update the duration to be the time from start to now.
            ElixirStorageService.transaction {
                val durationUntilEarlyCancel = activeChallenge.start - Clock.System.now()
                activeChallenge.duration = durationUntilEarlyCancel
            }
        }
    }

    public object Server : org.bukkit.Server by server {
        public suspend infix fun broadcastMessage(component: Component) {
            val prefix = get<ElixirLang>().prefixes.entries.first().value.let(MiniMessage.miniMessage()::deserialize)

            component.children().asFlow().collect { child ->
                prefix.append(child) message server
            }
        }
    }

    private fun durationFor(type: ResourceChallengeType): Duration = when (type) {
        else -> Random.nextInt(6, 12).hours
    }

    private fun updateProgress(progression: ChallengeProgress) {
        val bossBar = bossBars[progression.elixirPlayer.player()] ?: return
        if (progression.completed && bossBar.progress() == 1.0F) return // No need to update.

        when (progression.completed) {
            true -> { bossBar.progress(1.0f); bossBar.color(BossBar.Color.GREEN) }
            false -> bossBar.progress(progression.progress.toFloat() / activeChallenge.requiredAmount)
        }
    }

    private fun incrementParticipant(
        participant: Player,
        amount: Long
    ) {
        ElixirStorageService.transaction {
            val challengeParticipant = ChallengeProgress.find { ChallengeProgression.id eq participant.uniqueId and (ChallengeProgression.relational eq activeChallenge.id) }
                .firstOrNull()
                ?: ChallengeProgress.new(participant.uniqueId) { challenge = activeChallenge }

            challengeParticipant.incrementProgress(amount)
            updateProgress(challengeParticipant)
        }
    }

    private fun decrementParticipant(
        participant: Player,
        amount: Long
    ) {
        ElixirStorageService.transaction {
            val challengeParticipant = ChallengeProgress.find { ChallengeProgression.id eq participant.uniqueId and (ChallengeProgression.relational eq activeChallenge.id) }
                .firstOrNull()
                ?: ChallengeProgress.new(participant.uniqueId) { challenge = activeChallenge }

            challengeParticipant.decrementProgress(amount)
            updateProgress(challengeParticipant)
        }
    }
}

package dev.racci.elixir.core.services

import cloud.commandframework.Description
import cloud.commandframework.arguments.flags.CommandFlag
import cloud.commandframework.arguments.standard.EnumArgument
import cloud.commandframework.arguments.standard.IntegerArgument
import cloud.commandframework.arguments.standard.StringArgument
import cloud.commandframework.bukkit.parsers.PlayerArgument
import cloud.commandframework.context.CommandContext
import cloud.commandframework.exceptions.InvalidCommandSenderException
import cloud.commandframework.exceptions.InvalidSyntaxException
import cloud.commandframework.exceptions.NoPermissionException
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator
import cloud.commandframework.kotlin.coroutines.extension.suspendingHandler
import cloud.commandframework.kotlin.extension.buildAndRegister
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler
import cloud.commandframework.minecraft.extras.RichDescription
import cloud.commandframework.paper.PaperCommandManager
import cloud.commandframework.permission.OrPermission
import dev.racci.elixir.api.data.ElixirPlayer
import dev.racci.elixir.core.Elixir
import dev.racci.elixir.core.constants.ElixirPermission
import dev.racci.elixir.core.data.ElixirConfig
import dev.racci.elixir.core.data.ElixirLang
import dev.racci.elixir.core.modules.OpalsModule
import dev.racci.elixir.core.modules.OpalsModule.format
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.message
import dev.racci.minix.api.extensions.parse
import dev.racci.minix.api.extensions.server
import dev.racci.minix.api.services.DataService
import dev.racci.minix.api.services.DataService.Companion.inject
import dev.racci.minix.api.utils.Closeable
import dev.racci.minix.api.utils.adventure.PartialComponent
import dev.racci.minix.core.services.DataServiceImpl
import io.papermc.paper.world.MoonPhase
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.koin.core.component.get
import kotlin.jvm.optionals.getOrElse

@MappedExtension(Elixir::class, "Command Service", [ElixirStorageService::class])
public class CommandService(override val plugin: Elixir) : Extension<Elixir>() {
    private val elixirLang by DataService.inject().inject<ElixirLang>()
    internal val manager = object : Closeable<PaperCommandManager<CommandSender>>() {
        override fun create(): PaperCommandManager<CommandSender> {
            val coordinator = AsynchronousCommandExecutionCoordinator
                .newBuilder<CommandSender>()
                .withExecutor(dispatcher.get().executor)
                .withAsynchronousParsing()
                .build()

            val manager = PaperCommandManager.createNative(plugin, coordinator)

            MinecraftExceptionHandler<CommandSender>()
                .withHandler(MinecraftExceptionHandler.ExceptionType.INVALID_SYNTAX) { _, err ->
                    when (err) {
                        is InvalidCommandSenderException -> elixirLang.commands.invalidSender[
                            "sender" to { err.requiredSender.simpleName }
                        ]
                        is NoPermissionException -> elixirLang.commands.noPermission[
                            "permission" to { err.missingPermission }
                        ]
                        is InvalidSyntaxException -> elixirLang.commands.invalidSyntax[
                            "syntax" to { err.correctSyntax }
                        ]
                        else -> elixirLang.commands.executionError[
                            "reason" to { err.message.toString() }
                        ]
                    }
                }
                .withHandler(MinecraftExceptionHandler.ExceptionType.COMMAND_EXECUTION) { _, e ->
                    logger.error(e) { "An error occurred while executing a command" }
                    elixirLang.commands.executionError["error" to { e.message ?: "unknown" }]
                }
                .withHandler(MinecraftExceptionHandler.ExceptionType.NO_PERMISSION) { _, e ->
                    elixirLang.commands.noPermission["permission" to { e.message ?: "unknown" }]
                }
                .withHandler(MinecraftExceptionHandler.ExceptionType.INVALID_SENDER) { _, _ ->
                    elixirLang.commands.invalidSender.get()
                }
                .withDecorator { component ->
                    MiniMessage.miniMessage().parse(elixirLang.prefixes.firstNotNullOf { it.value }, false).append(component)
                }.apply(manager) { it }

            return manager
        }
    }
    private val playerFlag = CommandFlag.newBuilder("player")
        .withDescription(RichDescription.of(elixirLang.commands.connectionPlayerFlagDescription.get()))
        .withPermission(ElixirPermission.CONNECTION_TOGGLE_OTHERS.permission)
        .withAliases("p")
        .withArgument(PlayerArgument.newBuilder<Player>("player").asOptional().build())
    private val opalIntArg = IntegerArgument.newBuilder<CommandSender?>("amount").withMin(0).withSuggestionsProvider { _, _ ->
        val suggestions = arrayOfNulls<String>(25)
        repeat(25) { i ->
            suggestions[i] = (i * 25).toString()
        }
        suggestions.asList()
    }

    override suspend fun handleEnable() {
        registerOpalCommands()
        registerJoinLeaveMessage()

        manager.get().buildAndRegister(
            "howl",
            RichDescription.empty(),
            emptyArray()
        ) {
            this.mutate { it.flag(playerFlag) }
            this.handler { ctx ->
                if (server.worlds.first().moonPhase != MoonPhase.FULL_MOON) {
                    elixirLang.commands.howlNotFullMoon.get() message ctx.sender
                    return@handler
                }

                val sound = Sound.sound(Key.key("entity.wolf.howl"), Sound.Source.MASTER, 1f, 0.7f)
                server.playSound(sound, Sound.Emitter.self())
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun registerOpalCommands() {
        manager.get().buildAndRegister(
            "opals",
            RichDescription.of(Component.empty()),
            emptyArray()
        ) {
            this.registerCopy("shop", RichDescription.of(Component.empty())) {
                commandPermission = ElixirPermission.OPALS_SHOP.permission
                mutate { it.flag(playerFlag) }
                argument {
                    StringArgument.newBuilder<CommandSender>("shop")
                        .asOptionalWithDefault("main")
                        .withSuggestionsProvider { _, _ -> OpalsModule.menus.keys.toList() }.build()
                }
                handler { ctx ->
                    val player = ctx.flags().getValue<Player>("player").getOrElse { ctx.sender as Player }
                    OpalsModule.openShop(player, ctx.get("shop"))
                }
            }

            this.registerCopy("get", RichDescription.of(Component.empty())) {
                commandPermission = ElixirPermission.OPALS_GET.permission
                mutate { it.flag(playerFlag) }
                suspendingHandler { ctx ->
                    val target = ctx.flags().getValue<Player>("player").getOrElse { ctx.sender as Player }
                    val amount = ElixirStorageService.transaction { ElixirPlayer[target.uniqueId].opals }

                    elixirLang.commands.opalsGet[
                        "target" to { getTargetComponent(target, ctx, true) },
                        "amount" to { amount.format() }
                    ] message ctx.sender
                }
            }

            this.registerCopy("set", RichDescription.of(Component.empty())) {
                commandPermission = ElixirPermission.OPALS_MUTATE.permission
                mutate { it.flag(playerFlag) }
                argument(opalIntArg)
                suspendingHandler { ctx ->
                    val target = ctx.flags().getValue<Player>("player").getOrElse { ctx.sender as Player }
                    val amount = ctx.get<Int>("amount")

                    val (old, new) = ElixirStorageService.transaction {
                        with(ElixirPlayer[target.uniqueId]) {
                            val old = opals
                            opals = amount
                            old to opals
                        }
                    }

                    elixirLang.commands.opalsMutate[
                        "target" to { getTargetComponent(target, ctx, true) },
                        "previous" to { old },
                        "new" to { new }
                    ] message ctx.sender
                }
            }

            this.registerCopy("give", RichDescription.of(Component.empty())) {
                commandPermission = ElixirPermission.OPALS_MUTATE.permission
                mutate { it.flag(playerFlag) }
                argument(opalIntArg)
                suspendingHandler { ctx ->
                    val target = ctx.flags().getValue<Player>("player").getOrElse { ctx.sender as Player }
                    val amount = ctx.get<Int>("amount")

                    val (old, new) = ElixirStorageService.transaction {
                        with(ElixirPlayer[target.uniqueId]) {
                            val old = opals
                            opals += amount
                            old to opals
                        }
                    }

                    elixirLang.commands.opalsMutate[
                        "target" to { getTargetComponent(target, ctx, true) },
                        "previous" to { old },
                        "new" to { new }
                    ] message ctx.sender
                }
            }

            this.registerCopy("take", RichDescription.of(Component.empty())) {
                commandPermission = ElixirPermission.OPALS_MUTATE.permission
                mutate { it.flag(playerFlag) }
                argument(opalIntArg)
                suspendingHandler { ctx ->
                    val target = ctx.flags().getValue<Player>("player").getOrElse { ctx.sender as Player }
                    val amount = ctx.get<Int>("amount")

                    val (old, new) = ElixirStorageService.transaction {
                        with(ElixirPlayer[target.uniqueId]) {
                            val old = opals
                            opals -= amount
                            old to opals
                        }
                    }

                    elixirLang.commands.opalsMutate[
                        "target" to { getTargetComponent(target, ctx, true) },
                        "previous" to { old },
                        "new" to { new }
                    ] message ctx.sender
                }
            }

            handler { ctx -> OpalsModule.openShop(ctx.flags().getValue<Player>("player").getOrElse { ctx.sender as Player }) }
        }
    }

    private fun registerJoinLeaveMessage() {
        manager.get().buildAndRegister(
            "elixir",
            RichDescription.of(elixirLang.commands.connectionDescription.get()),
            emptyArray()
        ) {
            this.registerCopy("reload", Description.of("Reload the plugin")) {
                permission(ElixirPermission.RELOAD.permission)
                suspendingHandler(supervisor, dispatcher.get()) { handleReload() }
            }

            this.registerCopy("toggle", RichDescription.of(elixirLang.commands.connectionToggleDescription.get())) {
                permission(OrPermission.of(listOf(ElixirPermission.CONNECTION_TOGGLE.permission, ElixirPermission.CONNECTION_TOGGLE_OTHERS.permission)))
                mutate { it.flag(playerFlag) }
                suspendingHandler(supervisor, dispatcher.get()) { handleToggleMessage(it) }
            }

            this.registerCopy("enable", RichDescription.of(elixirLang.commands.connectionEnableDescription.get())) {
                permission(OrPermission.of(listOf(ElixirPermission.CONNECTION_TOGGLE.permission, ElixirPermission.CONNECTION_TOGGLE_OTHERS.permission)))
                mutate { it.flag(playerFlag) }
                suspendingHandler(supervisor, dispatcher.get()) { handleEnableMessage(it) }
            }

            this.registerCopy("disable", RichDescription.of(elixirLang.commands.connectionDisableDescription.get())) {
                permission(OrPermission.of(listOf(ElixirPermission.CONNECTION_TOGGLE.permission, ElixirPermission.CONNECTION_TOGGLE_OTHERS.permission)))
                mutate { it.flag(playerFlag) }
                suspendingHandler(supervisor, dispatcher.get()) { handleDisableMessage(it) }
            }

            this.registerCopy("mutate", RichDescription.of(elixirLang.commands.connectionMutateDescription.get())) {
                permission(OrPermission.of(listOf(ElixirPermission.CONNECTION_MUTATE.permission, ElixirPermission.CONNECTION_MUTATE_OTHERS.permission)))
                mutate { it.flag(playerFlag) }
                flag(
                    "connectionType",
                    arrayOf("t"),
                    RichDescription.of(elixirLang.commands.connectionTypeFlagDescription.get()),
                    EnumArgument.of(ConnectionMessage::class.java, "type")
                )
                flag(
                    "message",
                    arrayOf("m"),
                    RichDescription.of(elixirLang.commands.connectionMessageFlagDescription.get()),
                    StringArgument.greedy("message")
                )
                suspendingHandler(supervisor, dispatcher.get()) { handleMutateMessage(it) }
            }
        }
    }

    private fun handleToggleMessage(context: CommandContext<CommandSender>) {
        logger.trace { "handleToggleMessage" }
        val target = getTarget(context, ElixirPermission.CONNECTION_TOGGLE, ElixirPermission.CONNECTION_TOGGLE_OTHERS) ?: return
        logger.trace { "target: $target" }

        val newValue = ElixirStorageService.transaction {
            val elixirPlayer = ElixirPlayer[target.uniqueId]
            elixirPlayer.disableConnectionMessages = !elixirPlayer.disableConnectionMessages
            elixirPlayer.disableConnectionMessages
        }

        logger.trace { "newValue: $newValue" }

        elixirLang.commands.connectionToggle[
            "target" to { getTargetComponent(target, context) },
            "status" to { if (newValue) Component.text("Disabled").color(NamedTextColor.RED) else Component.text("Enabled").color(NamedTextColor.GREEN) }
        ] message context.sender
    }

    private fun handleEnableMessage(context: CommandContext<CommandSender>) {
        logger.trace { "handleEnableMessage" }
        val target = getTarget(context, ElixirPermission.CONNECTION_TOGGLE, ElixirPermission.CONNECTION_TOGGLE_OTHERS) ?: return
        logger.trace { "target: $target" }

        ElixirStorageService.transaction {
            ElixirPlayer[target.uniqueId].disableConnectionMessages = false
        }

        elixirLang.commands.connectionToggle[
            "target" to { getTargetComponent(target, context) },
            "status" to { Component.text("Enabled").color(NamedTextColor.GREEN) }
        ] message context.sender
    }

    private fun handleDisableMessage(context: CommandContext<CommandSender>) {
        logger.trace { "handleDisableMessage" }
        val target = getTarget(context, ElixirPermission.CONNECTION_TOGGLE, ElixirPermission.CONNECTION_TOGGLE_OTHERS) ?: return
        logger.trace { "target: $target" }

        ElixirStorageService.transaction {
            ElixirPlayer[target.uniqueId].disableConnectionMessages = true
        }

        elixirLang.commands.connectionToggle[
            "target" to { getTargetComponent(target, context) },
            "status" to { Component.text("Disabled").color(NamedTextColor.RED) }
        ] message context.sender
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun handleMutateMessage(context: CommandContext<CommandSender>) {
        val target = getTarget(context, ElixirPermission.CONNECTION_MUTATE, ElixirPermission.CONNECTION_MUTATE_OTHERS) ?: return

        val type = context.flags().getValue<ConnectionMessage>("connectionType").getOrElse {
            context.sender.sendMessage(elixirLang.commands.missingArgument["argument" to { "--connectionType" }])
            return
        } ?: error("Connection type is null")

        val message = context.flags().getValue<String>("message").getOrElse {
            context.sender.sendMessage(elixirLang.commands.missingArgument["argument" to { "--message" }])
            return
        } ?: error("Message is null")

        val (oldMessage, newMessage) = ElixirStorageService.transaction {
            val elixirPlayer = ElixirPlayer[target.uniqueId]
            when (type) {
                ConnectionMessage.JOIN -> {
                    val oldMessage = elixirPlayer.joinMessage
                    elixirPlayer.joinMessage = MiniMessage.miniMessage().deserialize(message)
                    oldMessage to elixirPlayer.joinMessage
                }
                ConnectionMessage.LEAVE -> {
                    val oldMessage = elixirPlayer.leaveMessage
                    elixirPlayer.leaveMessage = MiniMessage.miniMessage().deserialize(message)
                    oldMessage to elixirPlayer.leaveMessage
                }
            }
        }

        logger.trace { "oldMessage: $oldMessage" }
        logger.trace { "newMessage: $newMessage" }

        elixirLang.commands.connectionMutated[
            "target" to { getTargetComponent(target, context) },
            "type" to { type.name.lowercase() },
            "old" to { componentWithCopy(oldMessage) },
            "new" to { componentWithCopy(newMessage) }
        ] message context.sender
    }

    private fun handleReload() {
        val service = get<DataServiceImpl>()

        service.configDataHolder.invalidate(ElixirLang::class)
        service.configDataHolder.invalidate(ElixirConfig::class)

        service.configDataHolder[ElixirLang::class]
        service.configDataHolder[ElixirConfig::class]
    }

    private fun componentWithCopy(component: Component?): Component {
        if (component == null) return Component.text("null")

        val serialized = MiniMessage.miniMessage().serialize(component)
        val hoverEvent = HoverEvent.showText(Component.text("Click to copy").color(NamedTextColor.GRAY))
        return component.clickEvent(ClickEvent.copyToClipboard(serialized)).hoverEvent(hoverEvent)
    }

    private fun getTargetComponent(
        target: Player,
        context: CommandContext<CommandSender>,
        useNoun: Boolean = false
    ) = if (context.sender === target) {
        if (useNoun) Component.text("You") else Component.text("your")
    } else if (useNoun) target.displayName() else target.displayName().append(Component.text("'s"))

    private fun getTarget(
        context: CommandContext<CommandSender>,
        selfPermission: ElixirPermission,
        otherPermission: ElixirPermission
    ): Player? {
        val target = context.flags().getValue<CommandSender>("player").orElse(context.sender)
        logger.trace { "target: $target" }

        if (target === context.sender && !context.hasPermission(selfPermission.permission)) {
            elixirLang.commands.noPermission["permission" to selfPermission::permissionString] message context.sender
            logger.debug { "Player ${context.sender.name} tried to execute command without permission ${selfPermission.permission}" }
            return null
        }

        if (target !== context.sender && !context.hasPermission(otherPermission.permission)) {
            elixirLang.commands.noPermission["selfPermission" to otherPermission::permissionString] message context.sender
            logger.debug { "Player ${context.sender.name} tried to execute command without permission ${otherPermission.permission}" }
            return null
        }

        if (target !is Player) {
            elixirLang.commands.invalidPlayer["player" to target::getName] message context.sender
            logger.debug { "Player ${context.sender.name} tried to execute command on non-player ${target.name}" }
            return null
        }

        return target
    }

    public enum class ConnectionMessage(public val langFunc: ElixirLang.() -> PartialComponent) {
        JOIN(ElixirLang::defaultJoinMessage),
        LEAVE(ElixirLang::defaultLeaveMessage);
    }
}

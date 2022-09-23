package dev.racci.elixir.core.services

import cloud.commandframework.arguments.flags.CommandFlag
import cloud.commandframework.arguments.standard.EnumArgument
import cloud.commandframework.arguments.standard.StringArgument
import cloud.commandframework.bukkit.parsers.PlayerArgument
import cloud.commandframework.context.CommandContext
import cloud.commandframework.exceptions.InvalidCommandSenderException
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator
import cloud.commandframework.kotlin.coroutines.extension.suspendingHandler
import cloud.commandframework.kotlin.extension.buildAndRegister
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler
import cloud.commandframework.minecraft.extras.RichDescription
import cloud.commandframework.paper.PaperCommandManager
import cloud.commandframework.permission.OrPermission
import dev.racci.elixir.core.Elixir
import dev.racci.elixir.core.constants.ElixirPermission
import dev.racci.elixir.core.data.ElixirLang
import dev.racci.elixir.core.data.ElixirPlayer
import dev.racci.elixir.core.data.ElixirPlayer.Companion.threadContext
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.message
import dev.racci.minix.api.extensions.parse
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.services.DataService
import dev.racci.minix.api.services.DataService.Companion.inject
import dev.racci.minix.api.utils.Closeable
import dev.racci.minix.api.utils.adventure.PartialComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.jvm.optionals.getOrElse

@MappedExtension(Elixir::class, "Command Service")
class CommandService(override val plugin: Elixir) : Extension<Elixir>() {
    private val elixirLang by DataService.inject().inject<ElixirLang>()
    private val manager = object : Closeable<PaperCommandManager<CommandSender>>() {
        override fun create(): PaperCommandManager<CommandSender> {
            val coordinator = AsynchronousCommandExecutionCoordinator
                .newBuilder<CommandSender>()
                .withExecutor(threadContext.get().executor)
                .withAsynchronousParsing()
                .build()

            val manager = PaperCommandManager.createNative(plugin, coordinator)

            MinecraftExceptionHandler<CommandSender>()
                .withHandler(MinecraftExceptionHandler.ExceptionType.INVALID_SYNTAX) { _, e ->
                    val exception = e.castOrThrow<InvalidCommandSenderException>()

                    elixirLang.commands.invalidSyntax[
                        "command" to { exception.currentChain.getOrNull(0)?.name ?: "unknown" },
                        "args" to { exception.command?.arguments.orEmpty().joinToString(" ") { "<${it.name})>" } }
                    ]
                }
                .withHandler(MinecraftExceptionHandler.ExceptionType.COMMAND_EXECUTION) { _, e ->
                    elixirLang.commands.executionError["error" to { e.message ?: "unknown" }]
                }
                .withHandler(MinecraftExceptionHandler.ExceptionType.NO_PERMISSION) { _, e ->
                    elixirLang.commands.noPermission["permission" to { e.message ?: "unknown" }]
                }
                .withHandler(MinecraftExceptionHandler.ExceptionType.INVALID_SENDER) { _, e ->
                    elixirLang.commands.invalidSender.get()
                }
                .withDecorator { component ->
                    MiniMessage.miniMessage().parse(elixirLang.prefixes.firstNotNullOf { it.value }, false).append(component)
                }.apply(manager) { it }

            return manager
        }
    }

    override suspend fun handleEnable() {
        registerJoinLeaveMessage()
    }

    private fun registerJoinLeaveMessage() {
        manager.get().buildAndRegister(
            "elixir",
            RichDescription.of(elixirLang.commands.connectionDescription.get()),
            emptyArray()
        ) {
            val playerFlag = CommandFlag.newBuilder("player")
                .withDescription(RichDescription.of(elixirLang.commands.connectionPlayerFlagDescription.get()))
                .withPermission(ElixirPermission.CONNECTION_TOGGLE_OTHERS.permission)
                .withAliases("p")
                .withArgument(PlayerArgument.newBuilder<Player>("player").asOptional().build())

            this.registerCopy("toggle", RichDescription.of(elixirLang.commands.connectionToggleDescription.get())) {
                permission(OrPermission.of(listOf(ElixirPermission.CONNECTION_TOGGLE.permission, ElixirPermission.CONNECTION_TOGGLE_OTHERS.permission)))
                mutate { it.flag(playerFlag) }
                suspendingHandler(supervisor, threadContext.get()) { handleToggleMessage(it) }
            }

            this.registerCopy("enable", RichDescription.of(elixirLang.commands.connectionEnableDescription.get())) {
                permission(OrPermission.of(listOf(ElixirPermission.CONNECTION_TOGGLE.permission, ElixirPermission.CONNECTION_TOGGLE_OTHERS.permission)))
                mutate { it.flag(playerFlag) }
                suspendingHandler(supervisor, threadContext.get()) { handleEnableMessage(it) }
            }

            this.registerCopy("disable", RichDescription.of(elixirLang.commands.connectionDisableDescription.get())) {
                permission(OrPermission.of(listOf(ElixirPermission.CONNECTION_TOGGLE.permission, ElixirPermission.CONNECTION_TOGGLE_OTHERS.permission)))
                mutate { it.flag(playerFlag) }
                suspendingHandler(supervisor, threadContext.get()) { handleDisableMessage(it) }
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
                suspendingHandler(supervisor, threadContext.get()) { handleMutateMessage(it) }
            }
        }
    }

    private suspend fun handleToggleMessage(context: CommandContext<CommandSender>) {
        logger.trace { "handleToggleMessage" }
        val target = getTarget(context, ElixirPermission.CONNECTION_TOGGLE, ElixirPermission.CONNECTION_TOGGLE_OTHERS) ?: return
        logger.trace { "target: $target" }

        val newValue = transaction(getKoin().getProperty(Elixir.KOIN_DATABASE)) {
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

    private suspend fun handleEnableMessage(context: CommandContext<CommandSender>) {
        logger.trace { "handleEnableMessage" }
        val target = getTarget(context, ElixirPermission.CONNECTION_TOGGLE, ElixirPermission.CONNECTION_TOGGLE_OTHERS) ?: return
        logger.trace { "target: $target" }

        transaction(getKoin().getProperty(Elixir.KOIN_DATABASE)) {
            ElixirPlayer[target.uniqueId].disableConnectionMessages = false
        }

        elixirLang.commands.connectionToggle[
            "target" to { getTargetComponent(target, context) },
            "status" to { Component.text("Enabled").color(NamedTextColor.GREEN) }
        ] message context.sender
    }

    private suspend fun handleDisableMessage(context: CommandContext<CommandSender>) {
        logger.trace { "handleDisableMessage" }
        val target = getTarget(context, ElixirPermission.CONNECTION_TOGGLE, ElixirPermission.CONNECTION_TOGGLE_OTHERS) ?: return
        logger.trace { "target: $target" }

        transaction(getKoin().getProperty(Elixir.KOIN_DATABASE)) {
            ElixirPlayer[target.uniqueId].disableConnectionMessages = true
        }

        elixirLang.commands.connectionToggle[
            "target" to { getTargetComponent(target, context) },
            "status" to { Component.text("Disabled").color(NamedTextColor.RED) }
        ] message context.sender
    }

    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun handleMutateMessage(context: CommandContext<CommandSender>) {
        val target = getTarget(context, ElixirPermission.CONNECTION_MUTATE, ElixirPermission.CONNECTION_MUTATE_OTHERS) ?: return

        val type = context.flags().getValue<ConnectionMessage>("connectionType").getOrElse {
            context.sender.sendMessage(elixirLang.commands.missingArgument["argument" to { "--connectionType" }])
            return
        } ?: error("Connection type is null")

        val message = context.flags().getValue<String>("message").getOrElse {
            context.sender.sendMessage(elixirLang.commands.missingArgument["argument" to { "--message" }])
            return
        } ?: error("Message is null")

        val (oldMessage, newMessage) = transaction(getKoin().getProperty(Elixir.KOIN_DATABASE)) {
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

    private fun componentWithCopy(component: Component?): Component {
        if (component == null) return Component.text("null")

        val serialized = MiniMessage.miniMessage().serialize(component)
        val hoverEvent = HoverEvent.showText(Component.text("Click to copy").color(NamedTextColor.GRAY))
        return component.clickEvent(ClickEvent.copyToClipboard(serialized)).hoverEvent(hoverEvent)
    }

    private fun getTargetComponent(
        target: Player,
        context: CommandContext<CommandSender>
    ): Component = if (target === context.sender) Component.text("Your") else target.displayName().append(Component.text("'s"))

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

    enum class ConnectionMessage(val langFunc: ElixirLang.() -> PartialComponent) {
        JOIN(ElixirLang::defaultJoinMessage),
        LEAVE(ElixirLang::defaultLeaveMessage);
    }
}

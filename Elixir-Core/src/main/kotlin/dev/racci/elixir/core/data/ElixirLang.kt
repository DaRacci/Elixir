package dev.racci.elixir.core.data

import dev.racci.elixir.core.Elixir
import dev.racci.minix.api.annotations.MappedConfig
import dev.racci.minix.api.data.LangConfig
import dev.racci.minix.api.utils.adventure.PartialComponent
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
@MappedConfig(Elixir::class, "lang.conf")
class ElixirLang : LangConfig<Elixir>() {
    override val prefixes = mapOf(
        "elixir" to "<gradient:#ED13D9:#12d3ff>Elixir <white>» <reset>"
    )

    var defaultJoinMessage = PartialComponent.of("<prefix:elixir><player> has joined the server!")
    var defaultLeaveMessage = PartialComponent.of("<prefix:elixir><player> has left the server!")

    var commands = Commands()

    var opalShop = OpalShop()

    var eggTracker = EggTracker()

    @ConfigSerializable
    class Commands : InnerLang() {
        var reloadSuccess = PartialComponent.of("<prefix:elixir><aqua>Successfully reloaded the plugin in <time>!")
        var reloadFailure = PartialComponent.of("<prefix:elixir><red>Failed to reload the plugin!")

        var connectionToggle = PartialComponent.of("<prefix:elixir><aqua><target> connection status is now <status>!")
        var connectionMutated = PartialComponent.of("<prefix:elixir><aqua><target> connection status has been mutated from <old> to <new>!")

        var confirmationNeeded = PartialComponent.of("<prefix:elixir><red>This command requires confirmation, please run the command again to confirm!")
        var noConfirmationNeeded = PartialComponent.of("<prefix:elixir><red>You don't have any pending confirmations.")

        var reloadDescription = PartialComponent.of("Reloads the plugin")
        var connectionDescription = PartialComponent.of("<aqua>The main subcommand for managing connection messages.")
        var connectionToggleDescription = PartialComponent.of("<aqua>Toggles connection messages.")
        var connectionEnableDescription = PartialComponent.of("<aqua>Enables connection messages.")
        var connectionDisableDescription = PartialComponent.of("<aqua>Disables connection messages.")
        var connectionMutateDescription = PartialComponent.of("<aqua>Sets the connection message for the player.")

        var connectionPlayerFlagDescription = PartialComponent.of("<aqua>Selects the player to target, or if not specified, yourself.")
        var connectionTypeFlagDescription = PartialComponent.of("<aqua>The connection type to modify, either join or leave.")
        var connectionMessageFlagDescription = PartialComponent.of("<aqua>The message to set the connection message to.")

        var opalsGet = PartialComponent.of("<prefix:elixir><target> has <amount> opals!")
        var opalsMutate = PartialComponent.of("<prefix:elixir>Mutated <target> opals from <previous> to <new> opals!")

        var invalidSyntax = PartialComponent.of("<red>Invalid syntax! <gold>/<command> <args>")
        var executionError = PartialComponent.of("<red>An error occurred while executing this command!")
        var noPermission = PartialComponent.of("<red>You do not have permission to execute this command! (<permission>)")
        var invalidSender = PartialComponent.of("<red>You must be a player to execute this command!")
        var invalidPlayer = PartialComponent.of("<red>Couldn't find player <player>!")
        var missingArgument = PartialComponent.of("<red>Missing argument <arg>!")
    }

    @ConfigSerializable
    class OpalShop : InnerLang() {
        var itemNotPurchasable = PartialComponent.of("<red>This item cannot be purchasable currently!")
        var itemPurchasable = PartialComponent.of("<aqua>Left click to buy!")
        var itemAlreadyPurchased = PartialComponent.of("<red>You have already purchased this item!")
        var itemNotAffordable = arrayListOf(
            PartialComponent.of("<red>You cannot afford items!"),
            PartialComponent.of("<red>You need <aqua><needed></aqua>❖ more opals."),
            PartialComponent.of("<red>Get some at <aqua>store.elixirmc.net")
        )

        var purchaseSuccess = PartialComponent.of("<prefix:elixir><aqua>You have successfully purchased <item> for <price>!")
        var purchaseBroadcast = PartialComponent.of("<prefix:elixir><aqua><player> has purchased <item>!")
        var purchaseFailure = PartialComponent.of("<prefix:elixir><red>You need <needed> more opals to purchase <item>!")
        var purchaseFailureNoSpace = PartialComponent.of("<prefix:elixir><red>You do not have enough space in your inventory to purchase <item>!")
    }

    @ConfigSerializable
    class EggTracker : InnerLang() {
        var cannotDrop = PartialComponent.of("<prefix:elixir><red>You cannot drop the dragon egg!")
    }
}

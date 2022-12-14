package dev.racci.elixir.core.data

import dev.racci.elixir.core.Elixir
import dev.racci.minix.api.annotations.MappedConfig
import dev.racci.minix.api.data.LangConfig
import dev.racci.minix.api.utils.adventure.PartialComponent
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
@MappedConfig(Elixir::class, "lang.conf")
public class ElixirLang : LangConfig<Elixir>() {
    override val prefixes: Map<String, String> = mapOf(
        "elixir" to "<gradient:#ED13D9:#12d3ff>Elixir <white>» <reset>"
    )

    public var defaultJoinMessage: PartialComponent = PartialComponent.of("<prefix:elixir><player> has joined the server!")
    public var defaultLeaveMessage: PartialComponent = PartialComponent.of("<prefix:elixir><player> has left the server!")

    public var commands: Commands = Commands()

    public var opalShop: OpalShop = OpalShop()

    public var eggTracker: EggTracker = EggTracker()

    @ConfigSerializable
    public class Commands : InnerLang() {
        public var reloadSuccess: PartialComponent = PartialComponent.of("<prefix:elixir><aqua>Successfully reloaded the plugin in <time>!")
        public var reloadFailure: PartialComponent = PartialComponent.of("<prefix:elixir><red>Failed to reload the plugin!")

        public var connectionToggle: PartialComponent = PartialComponent.of("<prefix:elixir><aqua><target> connection status is now <status>!")
        public var connectionMutated: PartialComponent = PartialComponent.of("<prefix:elixir><aqua><target> connection status has been mutated from <old> to <new>!")

        public var confirmationNeeded: PartialComponent = PartialComponent.of("<prefix:elixir><red>This command requires confirmation, please run the command again to confirm!")
        public var noConfirmationNeeded: PartialComponent = PartialComponent.of("<prefix:elixir><red>You don't have any pending confirmations.")

        public var reloadDescription: PartialComponent = PartialComponent.of("Reloads the plugin")
        public var connectionDescription: PartialComponent = PartialComponent.of("<aqua>The main subcommand for managing connection messages.")
        public var connectionToggleDescription: PartialComponent = PartialComponent.of("<aqua>Toggles connection messages.")
        public var connectionEnableDescription: PartialComponent = PartialComponent.of("<aqua>Enables connection messages.")
        public var connectionDisableDescription: PartialComponent = PartialComponent.of("<aqua>Disables connection messages.")
        public var connectionMutateDescription: PartialComponent = PartialComponent.of("<aqua>Sets the connection message for the player.")

        public var connectionPlayerFlagDescription: PartialComponent = PartialComponent.of("<aqua>Selects the player to target, or if not specified, yourself.")
        public var connectionTypeFlagDescription: PartialComponent = PartialComponent.of("<aqua>The connection type to modify, either join or leave.")
        public var connectionMessageFlagDescription: PartialComponent = PartialComponent.of("<aqua>The message to set the connection message to.")

        public var opalsGet: PartialComponent = PartialComponent.of("<prefix:elixir><target> has <amount> opals!")
        public var opalsMutate: PartialComponent = PartialComponent.of("<prefix:elixir>Mutated <target> opals from <previous> to <new> opals!")

        public var invalidSyntax: PartialComponent = PartialComponent.of("<red>Invalid syntax! <gold>/<syntax>")
        public var executionError: PartialComponent = PartialComponent.of("<red>An error occurred while executing this command! (<reason>)")
        public var noPermission: PartialComponent = PartialComponent.of("<red>You do not have permission to execute this command! (<permission>)")
        public var invalidSender: PartialComponent = PartialComponent.of("<red>You must be of type <sender> to execute this command!")
        public var invalidPlayer: PartialComponent = PartialComponent.of("<red>Couldn't find player <player>!")
        public var missingArgument: PartialComponent = PartialComponent.of("<red>Missing argument <arg>!")

        public var howlNotFullMoon: PartialComponent = PartialComponent.of("<prefix:elixir><red>You can only howl during a full moon!")
    }

    @ConfigSerializable
    public class OpalShop : InnerLang() {
        public var itemNotPurchasable: PartialComponent = PartialComponent.of("<red>This item cannot be purchasable currently!")
        public var itemPurchasable: PartialComponent = PartialComponent.of("<aqua>Left click to buy!")
        public var itemAlreadyPurchased: PartialComponent = PartialComponent.of("<red>You have already purchased this item!")
        public var itemNotAffordable: ArrayList<PartialComponent> = arrayListOf(
            PartialComponent.of("<red>You cannot afford items!"),
            PartialComponent.of("<red>You need <aqua><needed></aqua>❖ more opals."),
            PartialComponent.of("<red>Get some at <aqua>store.elixirmc.net")
        )

        public var purchaseSuccess: PartialComponent = PartialComponent.of("<prefix:elixir><aqua>You have successfully purchased <item> for <price>!")
        public var purchaseBroadcast: PartialComponent = PartialComponent.of("<prefix:elixir><aqua><player> has purchased <item>!")
        public var purchaseFailure: PartialComponent = PartialComponent.of("<prefix:elixir><red>You need <needed> more opals to purchase <item>!")
        public var purchaseFailureNoSpace: PartialComponent = PartialComponent.of("<prefix:elixir><red>You do not have enough space in your inventory to purchase <item>!")
    }

    @ConfigSerializable
    public class EggTracker : InnerLang() {
        public var cannotDrop: PartialComponent = PartialComponent.of("<prefix:elixir><red>You cannot drop the dragon egg!")
        public var despawned: PartialComponent = PartialComponent.of("<prefix:elixir><red>The dragon egg has despawned!")
    }
}

package dev.racci.elixir.core.data

import dev.racci.elixir.core.Elixir
import dev.racci.minix.api.annotations.MappedConfig
import dev.racci.minix.api.data.MinixConfig
import dev.racci.minix.api.utils.PropertyFinder
import dev.racci.minix.api.utils.adventure.PartialComponent
import dev.racci.minix.api.utils.minecraft.BlockPos
import org.bukkit.entity.EntityType
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import org.spongepowered.configurate.objectmapping.meta.Required

@ConfigSerializable
@MappedConfig(Elixir::class, "elixir.conf")
class ElixirConfig : MinixConfig<Elixir>(true) {

    var modules: Modules = Modules()

    var guiButtons = GUI()

    @ConfigSerializable
    class GUI : InnerConfig by InnerConfig.Default() {
        var balance: GUIItemSlot = GUIItemSlot(
            "itemsadder:elixirmc__opal name:\"<white>Your Balance\"",
            "-1;1",
            listOf(
                PartialComponent.of("<aqua><amount>❖ <white>Opals"),
                PartialComponent.of(""),
                PartialComponent.of("<aqua>Get more at <light_purple>store.elixirmc.co")
            )
        )
        var back: GUIItemSlot = GUIItemSlot("itemsadder:mcicons__icon_cancel", "-1;5")
        var previousPage: GUIItemSlot = GUIItemSlot("itemsadder:mcicons__icon_left_blue", "-1;4")
        var nextPage: GUIItemSlot = GUIItemSlot("itemsadder:mcicons__icon_right_blue", "-1;6")

        @ConfigSerializable
        data class GUIItemSlot(
            @Required val display: String,
            @Required val position: String,
            val lore: List<PartialComponent> = emptyList(),
            val commands: List<String> = emptyList()
        )
    }

    @ConfigSerializable
    class Modules : PropertyFinder<Modules.ModuleConfig>(KeyMode.CAPITALISED) {
        @Comment("The TorchFire module sets entities on fire when attacked with a torch.")
        var torchFire = TorchFire()

        @Comment("The DrownConcrete module allows players to convert concrete powder to concrete by dropping it into water.")
        var drownConcrete = DrownConcrete()

        @Comment("Miscellaneous changes for beacons.")
        var enhanceBeacons = EnhanceBeacons()

        @Comment("Settings for handling the aether dimension.")
        var aether = Aether()

        @Comment("Settings for handling join messages.")
        var connectionMessage = ConnectionMessage()

        @Comment("Settings for handling the TPS of the server.")
        var tpsFixer = TPSFixer()

        @Comment("Settings for handling the Opals module.")
        var opals = Opals()

        var eggTracker = EggTracker()

        @Comment("Settings for handling hub worlds.")
        var hub = Hub()

        @ConfigSerializable
        class TorchFire : ModuleConfig() {
            var burnTicks: Int = 100
        }

        @ConfigSerializable
        class DrownConcrete : ModuleConfig()

        @ConfigSerializable
        class EnhanceBeacons : ModuleConfig() {
            var removeParticles: Boolean = true
        }

        @ConfigSerializable
        class Aether : ModuleConfig() {
            var worldName: String = "Aether"
            var jumpBoost: Int = 3
            var slowFalling: Boolean = true

            var preventBlockMutate: Boolean = true

            @Comment("If -1 disabled.")
            var preventVoid: Int = 52

            @Comment("Where to tp the player if they fall in the void, if null teleports to world spawn.")
            var portalCords: BlockPos? = null
        }

        @ConfigSerializable
        class ConnectionMessage : ModuleConfig()

        @ConfigSerializable
        class TPSFixer : ModuleConfig() {
            var mutateSpawnRate: MutableMap<EntityType, Float> = mutableMapOf(
                EntityType.BAT to 0f,
                EntityType.ENDERMAN to 0.5f,
                EntityType.BLAZE to 0.5f,
                EntityType.CAVE_SPIDER to 0.6f
            )

            @Comment("If -1 Disabled.\nThe TPS value at which the server will remove all entities from spawners.")
            var spawnerTPSThreshold: Double = 18.5

            @Comment("The float value to check rand against to determine if an entity should spawn.\nThe value for key -1 will be used as the else value.")
            var spawnTPSMultiplier: MutableMap<Double, Float> = mutableMapOf(
                19.0 to 1f,
                18.0 to 0.5f,
                17.0 to 0.25f,
                15.0 to 0.1f,
                -1.0 to 0f
            )
        }

        @ConfigSerializable
        class EggTracker : ModuleConfig()

        @ConfigSerializable
        class Opals : ModuleConfig() {
            var shop = Shop()

            @ConfigSerializable
            class Shop {
                @Required var title: PartialComponent = PartialComponent.of("<gradient:#ED13D9:#12d3ff>Opal Shop")

                @Required
                var menus: Map<String, Menu> = mapOf(
                    "test" to Menu().apply {
                        title = PartialComponent.of("<gradient:#ED13D9:#12d3ff>Opal Shop")
                        display = "itemsadder:iasurvival__end_sword"
                        position = "2;5"

                        elements = emptyMap()
                    }
                )
            }

            @ConfigSerializable
            class Menu {
                @Required var title: PartialComponent? = null

                @Required var display: String? = null

                @Required var position: String? = null

                var elements: Map<String, MenuElement> = mapOf()

                @ConfigSerializable
                class MenuElement {
                    var price: Int? = null

                    @Required var position: String? = null

                    @Required var display: String? = null

                    var guiName: PartialComponent? = null

                    var item: String? = null

                    var command: String? = null

                    var singleUse: Boolean? = null
                }
            }
        }

        // TODO -> Customise through commands
        @ConfigSerializable
        class Hub : ModuleConfig(false) {
            @Comment("The names of your hub worlds. (Case insensitive)")
            var worlds: List<String> = listOf("hub")

            @Comment("The base speed for players within the hub.")
            var speedMultiplier: Double = 1.3

            @Comment("The jump level for players within the hub.")
            var jumpLevel: Int = 2
        }

        open class ModuleConfig(var enabled: Boolean = true)
    }
}

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
        class Opals : ModuleConfig() {
            var shop = Shop()

            @ConfigSerializable
            class Shop {
                @Required var title: PartialComponent = PartialComponent.of("<gradient:#ED13D9:#12d3ff>Opal Shop")

                @Required
                var menus: Map<String, Menu> = mapOf(
                    "test" to Menu().apply {
                        title = PartialComponent.of("<gradient:#ED13D9:#12d3ff>Opal Shop")
                        display = "iasurvival:end_sword"
                        position = "3;5"

                        elements = mapOf(
                            "test:1" to Menu.MenuElement().apply {
                                position = "1;3"
                                display = "iavehicles:white_go_cart"
                                item = "iavehicles:orange_go_cart"
                                price = 1025
                                singleUse = true
                            },
                            "2" to Menu.MenuElement().apply {
                                position = "1;4"
                                display = "iavehicles:blue_go_cart"
                                command = "broadcast <player> bought a blue go cart!"
                                price = 500
                            }
                        )
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
                    @Required var price: Int? = null

                    @Required var position: String? = null

                    @Required var display: String? = null

                    var item: String? = null

                    var command: String? = null

                    var singleUse: Boolean? = null
                }
            }
        }

        open class ModuleConfig {
            var enabled: Boolean = true
        }
    }
}

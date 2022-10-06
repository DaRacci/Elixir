package dev.racci.elixir.core.data

import dev.racci.elixir.core.Elixir
import dev.racci.minix.api.annotations.MappedConfig
import dev.racci.minix.api.data.MinixConfig
import dev.racci.minix.api.utils.PropertyFinder
import dev.racci.minix.api.utils.minecraft.BlockPos
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
@MappedConfig(Elixir::class, "elixir.conf")
class ElixirConfig : MinixConfig<Elixir>(true) {

    var modules: Modules = Modules()

    @ConfigSerializable
    class Modules : PropertyFinder<Modules.ModuleConfig>() {
        @Comment("The TorchFire module sets entities on fire when attacked with a torch.")
        var torchFire = TorchFire()

        @Comment("The DrownConcrete module allows players to convert concrete powder to concrete by dropping it into water.")
        var drownConcrete = DrownConcrete()

        @Comment("Miscellaneous changes for beacons.")
        var enhanceBeacons = EnhanceBeacon()

        @Comment("Settings for handling the aether dimension.")
        var aether = AetherConfig()

        @Comment("Settings for handling join messages.")
        var joinMessages = ConnectionMessageConfig()

        @ConfigSerializable
        class TorchFire : ModuleConfig() {
            var burnTicks: Int = 100
        }

        @ConfigSerializable
        class DrownConcrete : ModuleConfig()

        @ConfigSerializable
        class EnhanceBeacon : ModuleConfig() {
            var removeParticles: Boolean = true
        }

        @ConfigSerializable
        class AetherConfig : ModuleConfig() {
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
        class ConnectionMessageConfig : ModuleConfig()

        open class ModuleConfig {
            var enabled: Boolean = true
        }
    }
}

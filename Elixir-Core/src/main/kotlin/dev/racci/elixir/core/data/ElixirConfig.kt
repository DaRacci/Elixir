package dev.racci.elixir.core.data

import dev.racci.elixir.core.Elixir
import dev.racci.minix.api.annotations.MappedConfig
import dev.racci.minix.api.data.MinixConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
@MappedConfig(Elixir::class, "elixir.conf")
class ElixirConfig : MinixConfig<Elixir>(true) {

    var modules: Modules = Modules()

    @ConfigSerializable
    class Modules {
        @Comment("The TorchFire module sets entities on fire when attacked with a torch.")
        var torchFire = TorchFire()

        @Comment("The DrownConcrete module allows players to convert concrete powder to concrete by dropping it into water.")
        var drownConcrete = DrownConcrete()

        @Comment("Miscellaneous changes for beacons.")
        var enhanceBeacons = EnhanceBeacon()

        @ConfigSerializable
        class TorchFire {
            var enabled: Boolean = true
            var burnTicks: Int = 100
        }

        @ConfigSerializable
        class DrownConcrete {
            var enabled: Boolean = true
        }

        @ConfigSerializable
        class EnhanceBeacon {
            var enabled: Boolean = true
            var removeParticles: Boolean = true
        }
    }
}

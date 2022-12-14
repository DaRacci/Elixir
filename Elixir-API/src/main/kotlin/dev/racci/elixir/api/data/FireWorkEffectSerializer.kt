package dev.racci.elixir.api.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.FireworkEffect

public object FireWorkEffectSerializer : KSerializer<FireworkEffect> {
    override fun deserialize(decoder: Decoder): FireworkEffect {
        TODO("Not yet implemented")
    }

    override val descriptor: SerialDescriptor
        get() = TODO("Not yet implemented")

    override fun serialize(encoder: Encoder, value: FireworkEffect) {
        TODO("Not yet implemented")
    }
}

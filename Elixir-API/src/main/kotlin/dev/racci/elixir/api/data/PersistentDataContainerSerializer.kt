package dev.racci.elixir.api.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.persistence.PersistentDataContainer

public object PersistentDataContainerSerializer : KSerializer<PersistentDataContainer> {
    override fun deserialize(decoder: Decoder): PersistentDataContainer {
        TODO("Not yet implemented")
    }

    override val descriptor: SerialDescriptor
        get() = TODO("Not yet implemented")

    override fun serialize(encoder: Encoder, value: PersistentDataContainer) {
        TODO("Not yet implemented")
    }
}

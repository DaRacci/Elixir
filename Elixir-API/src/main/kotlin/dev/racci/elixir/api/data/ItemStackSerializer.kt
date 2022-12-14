package dev.racci.elixir.api.data

import dev.racci.elixir.api.extensions.enumValueOfOrNull
import dev.racci.minix.api.serializables.PotionEffectSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import kotlin.properties.Delegates

public object ItemStackSerializer : KSerializer<ItemStack> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ItemStack") {
        element<Material>("type")
        element<Int>("amount", isOptional = true)
        element<ItemMeta>("meta", isOptional = true)
    }

    override fun serialize(
        encoder: Encoder,
        value: ItemStack
    ): Unit = encoder.encodeStructure(descriptor) {
        encodeStringElement(descriptor, 0, value.type.name)
        if (value.amount > 1) encodeIntElement(descriptor, 1, value.amount)
        if (value.hasItemMeta()) encodeSerializableElement(descriptor, 2, ItemMetaSerializer, value.itemMeta)
    }

    override fun deserialize(decoder: Decoder): ItemStack {
        var type: Material by Delegates.notNull()
        var amount: Int = 1
        var meta: ItemMeta? = null
        decoder.decodeStructure(PotionEffectSerializer.descriptor) {
            while (true) {
                when (val i = decodeElementIndex(PotionEffectSerializer.descriptor)) {
                    0 -> type = enumValueOfOrNull<Material>(decodeStringElement(descriptor, i)) ?: error("Invalid material name ${decodeStringElement(descriptor, i)}")
                    1 -> amount = decodeIntElement(descriptor, i)
                    2 -> meta = decodeSerializableElement(descriptor, i, ItemMetaSerializer)
                    CompositeDecoder.DECODE_DONE -> break
                }
            }
        }

        return ItemStack(type, amount).apply {
            if (meta != null) itemMeta = meta
        }
    }
}

package dev.racci.elixir.api.data

import dev.racci.minix.api.serializables.AttributeModifierSerializer
import dev.racci.minix.api.serializables.EnchantSerializer
import dev.racci.minix.api.serializables.LocationSerializer
import dev.racci.minix.api.serializables.MiniMessageSerializer
import dev.racci.minix.api.serializables.NamespacedKeySerializer
import dev.racci.minix.api.serializables.PatternSerializer
import dev.racci.minix.api.serializables.PotionEffectSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.Color
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.meta.ItemMeta

public object ItemMetaSerializer : KSerializer<ItemMeta> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ItemMeta") {
        element("display-name", MiniMessageSerializer.descriptor, isOptional = true)
        element("lore", ListSerializer(MiniMessageSerializer).descriptor, isOptional = true)
        element<Int>("model", isOptional = true)
        element("enchants", ListSerializer(EnchantSerializer).descriptor, isOptional = true)
        element("attributes", ListSerializer(AttributeModifierSerializer).descriptor, isOptional = true)
        element<Int>("repair-cost", isOptional = true)
        element<ItemFlag>("flags", isOptional = true)
        element<Boolean>("unbreakable", isOptional = true)
        element<Int>("damage", isOptional = true)
        element("pdc", PersistentDataContainerSerializer.descriptor, isOptional = true)

        // ArmorStand
        buildClassSerialDescriptor("ArmourStand") {
            element<Boolean>("invisible", isOptional = true)
            element<Boolean>("base-plate", isOptional = true)
            element<Boolean>("show-arms", isOptional = true)
            element<Boolean>("small", isOptional = true)
            element<Boolean>(".marker", isOptional = true)
        }.also { element("armour-stand", it, isOptional = true) }

        // AxolotlBucket
        element<Int>("axolotl.variant", isOptional = true)

        // Banner
        element<DyeColor>("banner.base", isOptional = true)
        element("banner.patterns", ListSerializer(PatternSerializer).descriptor, isOptional = true)

        // BlockState
        element<Material>("block-state.material", isOptional = true)

        // Book
        element<String>("book.title", isOptional = true)
        element<String>("book.author", isOptional = true)
        element("book.pages", ListSerializer(MiniMessageSerializer).descriptor, isOptional = true)
        element<Boolean>("book.resolved", isOptional = true)
        element<Int>("book.generation", isOptional = true)

        // Bundle
        element("bundle.items", ListSerializer(ItemStackSerializer).descriptor, isOptional = true)

        // Charge
        element("charge.effect", FireWorkEffectSerializer.descriptor, isOptional = true)

        // Compass
        element("compass.lodestone", LocationSerializer.descriptor, isOptional = true)
        element<Boolean>("compass.tracked", isOptional = true)

        // Crossbow
        element<Boolean>("crossbow.charged", isOptional = true)
        element("crossbow.charged-projectiles", ListSerializer(ItemStackSerializer).descriptor, isOptional = true)

        // EnchantedBook
        element("enchanted-book.enchants", ListSerializer(EnchantSerializer).descriptor, isOptional = true)

        // Firework
        element("firework.effects", ListSerializer(FireWorkEffectSerializer).descriptor, isOptional = true)
        element<Int>("firework.power", isOptional = true)

        // KnowledgeBook
        element("knowledge-book.recipes", ListSerializer(NamespacedKeySerializer).descriptor, isOptional = true)

        // LeatherArmor
        element<DyeColor>("leather-armor.color", isOptional = true)

        // Map
        element<Int>("map.id", isOptional = true)
        element<Boolean>("map.scaling", isOptional = true)
        element<String>("map.location-name", isOptional = true)
        element<Color>("map.color", isOptional = true)

        // Potion
        element<Boolean>("potion.type", isOptional = true)
        element("potion.effects", ListSerializer(PotionEffectSerializer).descriptor, isOptional = true)

        // Skull TODO

        // SuspiciousStew
        element("stew.effects", ListSerializer(PotionEffectSerializer).descriptor, isOptional = true)

        // TropicalFishBucket
        element<Int>("fish.variant", isOptional = true)
    }

    override fun deserialize(decoder: Decoder): ItemMeta {
        TODO("Not yet implemented")
    }

    override fun serialize(encoder: Encoder, value: ItemMeta) {
        TODO("Not yet implemented")
    }
}

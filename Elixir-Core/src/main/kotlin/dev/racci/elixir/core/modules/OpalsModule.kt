package dev.racci.elixir.core.modules

import com.willfp.eco.core.drops.DropQueue
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.TestableItem
import dev.racci.elixir.core.data.ElixirConfig
import dev.racci.elixir.core.data.ElixirLang
import dev.racci.elixir.core.data.ElixirPlayer
import dev.racci.elixir.core.data.ElixirPlayer.ElixirUser.opals
import dev.racci.minix.api.builders.ItemBuilderDSL
import dev.racci.minix.api.extensions.message
import dev.racci.minix.api.extensions.onlinePlayers
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.extensions.server
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.incendo.interfaces.core.click.ClickHandler
import org.incendo.interfaces.kotlin.paper.MutableChestPaneView
import org.incendo.interfaces.kotlin.paper.asElement
import org.incendo.interfaces.kotlin.paper.asViewer
import org.incendo.interfaces.kotlin.paper.buildChestInterface
import org.incendo.interfaces.paper.element.ItemStackElement
import org.incendo.interfaces.paper.pane.ChestPane
import org.incendo.interfaces.paper.type.ChestInterface
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.get

private interface ShopItem {
    fun giveTo(player: Player)
}

private class ItemShopItem(
    private val item: TestableItem
) : ShopItem {
    override fun giveTo(player: Player) =
        DropQueue(player)
            .addItem(item.item)
            .forceTelekinesis()
            .push()
}

private class CommandShopItem(
    private val command: String
) : ShopItem {
    override fun giveTo(player: Player) {
        Bukkit.dispatchCommand(
            Bukkit.getConsoleSender(),
            command.replace("<player>", player.name)
        )
    }
}

object OpalsModule : ModuleActor<ElixirConfig.Modules.Opals>() {
    private val menus = mutableMapOf<String, ChestInterface>()

    override suspend fun load() {
        buildShops()
    }

    fun openShop(
        player: Player,
        shopID: String = "main"
    ): Boolean {
        val shop = menus[shopID] ?: return false
        shop.open(player.asViewer())

        return true
    }

    private fun buildShops() {
        menus["main"] = buildChestInterface {
            rows = 5
            title = getConfig().shop.title.get()
            clickHandler = ClickHandler.cancel()

            withTransform { view ->
                val mask = """
                221111122
                333333333
                333333333
                221101122
                """.trim().lines()

                for (i in mask.indices) {
                    val char = mask[i]

                    val offset = 1
                    val row = Math.floorDiv(i - offset, 9) + offset
                    val col = (i - offset) % 9 + offset

                    logger.debug { "index: $i, col: $col, row: $row" }

                    val material = when (char) {
                        '1' -> Material.LIGHT_BLUE_STAINED_GLASS_PANE
                        '2' -> Material.BLACK_STAINED_GLASS_PANE
                        '3' -> Material.GRAY_STAINED_GLASS_PANE
                        else -> continue
                    }

                    val item = ItemBuilderDSL.from(material) {
                        name = Component.empty()
                    }.asElement<ChestPane>()

                    view[col, row] = item
                }
            }

            withTransform { view ->
                for ((id, menu) in getConfig().shop.menus) {
                    val (row, column) = menu.position!!.split(";").map { it.toInt() }
                    view[row, column] = Items.lookup(menu.display!!).item.asElement { ctx ->
                        if (!ctx.click().leftClick()) return@asElement
                    }

                    menus.computeIfAbsent(id) { shopMenu(menu) }
                }
            }
        }
    }

    private fun shopMenu(menu: ElixirConfig.Modules.Opals.Menu): ChestInterface {
        return buildChestInterface {
            rows = maxOf(minOf(1, menu.elements.size / 7), menu.elements.values.maxOf { it.position!!.split(";")[0].toInt() }) + 1
            val itemUpdates = mutableMapOf<Pair<Int, Int>, ItemStackElement<ChestPane>.(Player) -> ItemStack>()

            title = getConfig().shop.title.get().append(Component.text(" - ")).append(menu.title!!.get())
            clickHandler = ClickHandler.cancel()

            withTransform { view ->
                repeat(rows - 1) {
                    view.filler(Material.LIGHT_BLUE_STAINED_GLASS_PANE, it to 1..1, it to 9..9)
                }

                view.filler(Material.LIGHT_BLUE_STAINED_GLASS_PANE, rows to 1..4, rows to 6..9)

                view[rows, 5] = ItemBuilderDSL.from(Material.DIAMOND) {
                    name = MiniMessage.miniMessage().deserialize("<white>Your Balance:")
                }.asElement()
            }

            withTransform { view ->
                for ((id, element) in menu.elements) {
                    val (row, column) = element.position!!.split(";", limit = 1).map(String::toInt)
                    val shopItem = buySlot(id, element)

                    view[row, column] = shopItem
                    itemUpdates[row to column] = { viewer ->
                        val opalShop = get<ElixirLang>().opalShop
                        val opals = transaction { ElixirPlayer[viewer.uniqueId].opals }

                        val opalInfoLore = mutableListOf(
                            Component.empty(),
                            MiniMessage.miniMessage().deserialize("<white>Price: <aqua>${element.price}❖"),
                            Component.empty()
                        )

                        if (element.singleUse!! && transaction { (ElixirPlayer[viewer.uniqueId].purchases[id] ?: 0) > 0 }) {
                            opalInfoLore.add(opalShop.itemAlreadyPurchased.get())
                        } else if (opals >= element.price!!) {
                            opalInfoLore.add(opalShop.itemPurchasable.get())
                        } else {
                            opalInfoLore.addAll(opalShop.itemNotAffordable.map { it.get() })
                        }

                        ItemBuilderDSL.from(shopItem.itemStack().clone()) {
                            lore = this.lore + opalInfoLore
                        }
                    }
                }
            }

            addTransform { staticPane, view ->
                if (view.viewing()) return@addTransform staticPane

                val item = staticPane.element(rows, 5)
                val stack = item.itemStack().clone()
                val handler = item.clickHandler()

                var pane = ItemBuilderDSL.from(stack) {
                    lore(
                        MiniMessage.miniMessage().deserialize("<aqua>$opals❖ <white>Opals"),
                        Component.empty(),
                        MiniMessage.miniMessage().deserialize("<yellow>Get more at <magenta>store.elixirmc.co")
                    )
                }.asElement(handler).let { staticPane.element(it, rows, 5) }

                for ((position, action) in itemUpdates.entries) {
                    val curItem = pane.element(position.first, position.second)
                    val newItem = curItem.action(view.viewer().player())

                    pane = pane.element(newItem.asElement(curItem.clickHandler()), position.first, position.second)
                }

                pane
            }

            withCloseHandler { _, chestView ->
                menus["main"]!!.open(chestView.viewer())
            }
        }
    }

    private fun buySlot(
        id: String,
        element: ElixirConfig.Modules.Opals.Menu.MenuElement
    ): ItemStackElement<ChestPane> {
        val item = if (element.command.isNullOrBlank()) {
            ItemShopItem(Items.lookup(element.item!!))
        } else CommandShopItem(element.command!!)

        return ItemStackElement.of(Items.lookup(element.display!!).item) { ctx ->
            if (!ctx.click().leftClick()) return@of

            val player = ctx.cause().whoClicked.castOrThrow<Player>()
            var opals = 0
            var purchases: Int? = null
            transaction {
                opals = ElixirPlayer[player.uniqueId].opals
                purchases = ElixirPlayer[player.uniqueId].purchases[id]
            }

            if (element.singleUse == true && (purchases ?: 0) > 0) {
                return@of
            }

            if (element.price!! == 0) return@of

            if (opals < element.price!!) {
                get<ElixirLang>().opalShop.purchaseFailure[
                    "needed" to { element.price!! - opals },
                    "item" to Items.lookup(element.display!!).item::displayName
                ] message player

                player.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.MASTER, 1f, 0.9f))

                return@of
            }

            transaction {
                ElixirPlayer[player.uniqueId].opals -= element.price!!
                if (element.singleUse!!) ElixirPlayer[player.uniqueId].purchases[id] = (purchases ?: 0) + 1
            }

            logger.info { "${player.name} bought $id for ${element.price} opals." }

            item.giveTo(player)
            player.playSound(Sound.sound(Key.key("block.note_block.pling"), Sound.Source.MASTER, 1f, 1.5f))

            get<ElixirLang>().opalShop.purchaseSuccess[
                "item" to Items.lookup(element.display!!).item::displayName,
                "price" to element.price::toString
            ] message player

            get<ElixirLang>().opalShop.purchaseBroadcast[
                "player" to player::displayName,
                "item" to Items.lookup(element.display!!).item::displayName
            ] message server

            val sound = Sound.sound(Key.key("entity.player.levelup"), Sound.Source.MASTER, 2f, 1.5f)
            onlinePlayers.forEach { it.playSound(sound) }
        }
    }

    private fun MutableChestPaneView.filler(material: Material, vararg slots: Pair<Int, IntRange>) {
        val item = ItemBuilderDSL.from(material) {
            name = Component.empty()
        }.asElement<ChestPane>(ClickHandler.cancel())

        slots.flatMap { (row, range) -> range.map { row to it } }
            .forEach { (row, column) -> this[row, column] = item }
    }
}

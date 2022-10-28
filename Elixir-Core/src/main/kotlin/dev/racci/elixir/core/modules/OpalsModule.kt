package dev.racci.elixir.core.modules

import com.willfp.eco.core.drops.DropQueue
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.TestableItem
import com.willfp.eco.core.items.isEmpty
import com.willfp.eco.core.recipe.parts.EmptyTestableItem
import dev.racci.elixir.core.data.ElixirConfig
import dev.racci.elixir.core.data.ElixirLang
import dev.racci.elixir.core.data.ElixirPlayer
import dev.racci.minix.api.builders.ItemBuilderDSL
import dev.racci.minix.api.extensions.message
import dev.racci.minix.api.extensions.onlinePlayers
import dev.racci.minix.api.extensions.parse
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.extensions.scheduler
import dev.racci.minix.api.extensions.server
import dev.racci.minix.api.extensions.ticks
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.incendo.interfaces.core.Interface
import org.incendo.interfaces.core.arguments.ArgumentKey
import org.incendo.interfaces.core.arguments.HashMapInterfaceArguments
import org.incendo.interfaces.core.click.ClickHandler
import org.incendo.interfaces.core.pane.Pane
import org.incendo.interfaces.kotlin.arguments
import org.incendo.interfaces.kotlin.paper.GenericClickHandler
import org.incendo.interfaces.kotlin.paper.MutableChestPaneView
import org.incendo.interfaces.kotlin.paper.asElement
import org.incendo.interfaces.kotlin.paper.asViewer
import org.incendo.interfaces.kotlin.paper.buildChestInterface
import org.incendo.interfaces.paper.PlayerViewer
import org.incendo.interfaces.paper.element.ItemStackElement
import org.incendo.interfaces.paper.pane.ChestPane
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
    internal val playerArgumentKey = ArgumentKey.of("player", Player::class.java)
    internal val opalsArgumentKey = ArgumentKey.of("opals", Int::class.java)
    internal val menus = mutableMapOf<String, Interface<*, PlayerViewer>>()

    override suspend fun load() {
        buildShops()
    }

    fun openShop(
        player: Player,
        shopID: String = "main"
    ): Boolean {
        val shop = menus[shopID]

        if (shop == null) {
            logger.warn { "Attempted to open shop $shopID, but it does not exist!" }
            return false
        }

        shop.open(
            player.asViewer(),
            HashMapInterfaceArguments
                .with(playerArgumentKey, player)
                .with(opalsArgumentKey, transaction(getProperty("database")) { ElixirPlayer[player.uniqueId].opals })
                .build()
        )

        return true
    }

    private fun buildShops() {
        menus["main"] = buildChestInterface {
            rows = 4
            title = getConfig().shop.title.get()
            clickHandler = ClickHandler.cancel()

            withTransform { view ->
                view.mask(
                    0,
                    mapOf(
                        1 to Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                        2 to Material.BLACK_STAINED_GLASS_PANE,
                        3 to Material.GRAY_STAINED_GLASS_PANE
                    ),
                    """
                    221111122
                    333333333
                    333333333
                    221111122
                    """
                )
            }

            withTransform(2) { view ->
                if (!view.arguments.contains(opalsArgumentKey)) return@withTransform
                view.insertButtons(rows, view.arguments.get(opalsArgumentKey))
            }

            withTransform(3) { view ->
                for ((id, menu) in getConfig().shop.menus) {
                    val (row, column) = menu.position!!.split(";").map { it.toInt() }

                    logger.debug { "Id: $id" }
                    logger.debug { "Row: $row, Column: $column" }

                    logger.trace { Items.lookup(menu.display!!).item }
                    view[row, column] = Items.lookup(menu.display!!).item.asElement { ctx ->
                        if (!ctx.click().leftClick()) return@asElement

                        menus[id]!!.open(ctx.viewer())
                    }

                    menus.computeIfAbsent(id) { shopMenu(menu) }
                }
            }
        }
    }

    private fun shopMenu(menu: ElixirConfig.Modules.Opals.Menu): Interface<*, PlayerViewer> {
        return buildChestInterface {
            rows = maxOf(minOf(1, menu.elements.size / 7), menu.elements.values.maxOf { it.position!!.split(";")[0].toInt() }) + 2
            title = getConfig().shop.title.get().append(Component.text(" - ")).append(menu.title!!.get())
            clickHandler = ClickHandler.cancel()

            logger.debug { "Rows: $rows" }

            withTransform { view ->
                val mask = buildString {
                    var availableRows = rows - 2 // 2 for the top and bottom rows
                    append("221111122")

                    repeat(minOf(1, availableRows)) {
                        availableRows--
                        append("333333333")
                    }

                    if (availableRows <= 0) return@buildString
                    append("221101122")
                }

                view.mask(
                    0,
                    mapOf(
                        1 to Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                        2 to Material.BLACK_STAINED_GLASS_PANE,
                        3 to Material.GRAY_STAINED_GLASS_PANE
                    ),
                    mask
                )
            }

            withTransform(3) { view ->
                for ((id, element) in menu.elements) {
                    val (row, column) = element.position!!.split(";", limit = 2).map(String::toInt)
                    view[column, row] = buySlot(id, element)
                }
            }

            withTransform(4) { view ->
                if (!view.arguments.contains(playerArgumentKey)) return@withTransform

                val player = view.arguments.get(playerArgumentKey)
                val opals = view.arguments.get(opalsArgumentKey)

                view.insertButtons(rows, opals, "main")

                for ((id, element) in menu.elements) {
                    val (row, column) = element.position!!.split(";", limit = 2).map(String::toInt)
                    val staticElement = view[column, row]
                    if (staticElement.itemStack().isEmpty) continue

                    val opalShop = get<ElixirLang>().opalShop
                    val opalInfoLore = mutableListOf(
                        Component.empty(),
                        MiniMessage.miniMessage().parse("<white>Price: <aqua>${element.price}❖"),
                        Component.empty()
                    )

                    if (element.singleUse!! && transaction { (ElixirPlayer[player.uniqueId].purchases[id] ?: 0) > 0 }) {
                        opalInfoLore.add(opalShop.itemAlreadyPurchased.get())
                    } else if (opals >= element.price!!) {
                        opalInfoLore.add(opalShop.itemPurchasable.get())
                    } else {
                        opalInfoLore.addAll(opalShop.itemNotAffordable.map { it.get() })
                    }

                    ItemBuilderDSL.from(staticElement.itemStack().clone()) {
                        lore = this.lore + opalInfoLore
                    }.asElement(staticElement.clickHandler())
                }
            }

            withCloseHandler { _, view ->
                scheduler {
                    openShop(view.arguments.get(playerArgumentKey), "main")
                }.runTaskLater(plugin, 1.ticks)
            }
        }
    }

    private fun <P : Pane> buySlot(
        id: String,
        element: ElixirConfig.Modules.Opals.Menu.MenuElement
    ): ItemStackElement<P> {
        logger.trace { Items.lookup(element.item!!).item }

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
                    "item" to { Items.lookup(element.display!!).item.displayName() }
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
                "item" to { Items.lookup(element.display!!).item.displayName() },
                "price" to { element.price.toString() }
            ] message player

            get<ElixirLang>().opalShop.purchaseBroadcast[
                "player" to { player.displayName() },
                "item" to { Items.lookup(element.display!!).item.displayName() }
            ] message server

            val sound = Sound.sound(Key.key("entity.player.levelup"), Sound.Source.MASTER, 2f, 1.5f)
            onlinePlayers.forEach { it.playSound(sound) }
        }
    }

    private fun MutableChestPaneView.mask(
        offset: Int,
        materials: Map<Int, Material>,
        rawMask: String
    ) {
        val mask = rawMask.trim().toMutableList()
        mask.retainAll(Char::isDigit)

        for (i in mask.indices) {
            val char = mask[i]

            val row = Math.floorDiv(i - offset, 9) + offset
            val col = (i - offset) % 9 + offset

            logger.debug { "index: $i, col: $col, row: $row" }

            val material = materials[char.toString().toInt()] ?: continue

            this[col, row] = ItemBuilderDSL.from(material) {
                name = Component.empty()
            }.asElement()
        }
    }

    private fun MutableChestPaneView.insertButtons(
        y: Int,
        opals: Int,
        outerInventory: String? = null
    ) {
        fun placeButton(
            x: Int,
            y: Int,
            button: ElixirConfig.GUI.GUIItemSlot,
            action: GenericClickHandler<ChestPane>? = null
        ) {
            val item = Items.lookup(button.display)
            if (item is EmptyTestableItem) return logger.debug { "Empty item: ${button.display}" }

            this[x, y] = ItemBuilderDSL.from(item.item.clone()) {
                lore = button.lore.map { it["amount" to { opals }] }
            }.asElement(action)
        }

        val buttons = get<ElixirConfig>().guiButtons

        placeButton(0, y - 1, buttons.balance)
//        placeButton(3, y - 1, buttons.previousPage)
//        placeButton(5, y - 1, buttons.nextPage)

        placeButton(4, y - 1, buttons.back) { ctx ->
            if (outerInventory != null) {
                openShop(ctx.cause().whoClicked.castOrThrow(), outerInventory)
            } else ctx.cause().whoClicked.closeInventory()
        }
    }
}

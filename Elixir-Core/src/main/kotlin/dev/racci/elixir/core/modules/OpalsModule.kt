package dev.racci.elixir.core.modules

import com.willfp.eco.core.drops.DropQueue
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.TestableItem
import com.willfp.eco.core.recipe.parts.EmptyTestableItem
import dev.esophose.playerparticles.PlayerParticles
import dev.esophose.playerparticles.manager.ParticleStyleManager
import dev.esophose.playerparticles.particles.ParticleEffect
import dev.racci.elixir.core.data.ElixirConfig
import dev.racci.elixir.core.data.ElixirLang
import dev.racci.elixir.core.data.ElixirPlayer
import dev.racci.elixir.core.extensions.mask
import dev.racci.elixir.core.extensions.toVec
import dev.racci.elixir.core.services.ElixirStorageService
import dev.racci.elixir.core.utils.RenderablePaginatedTransform
import dev.racci.minix.api.builders.ItemBuilder
import dev.racci.minix.api.builders.ItemBuilderDSL
import dev.racci.minix.api.extensions.message
import dev.racci.minix.api.extensions.noItalic
import dev.racci.minix.api.extensions.onlinePlayers
import dev.racci.minix.api.extensions.parse
import dev.racci.minix.api.extensions.pm
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.extensions.scheduler
import dev.racci.minix.api.extensions.server
import dev.racci.minix.api.extensions.taskAsync
import dev.racci.minix.api.extensions.ticks
import kotlinx.coroutines.delay
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
import org.incendo.interfaces.core.util.Vector2
import org.incendo.interfaces.core.view.InterfaceView
import org.incendo.interfaces.kotlin.arguments
import org.incendo.interfaces.kotlin.paper.GenericClickHandler
import org.incendo.interfaces.kotlin.paper.MutableChestInterfaceBuilder
import org.incendo.interfaces.kotlin.paper.MutableChestPaneView
import org.incendo.interfaces.kotlin.paper.asElement
import org.incendo.interfaces.kotlin.paper.asViewer
import org.incendo.interfaces.kotlin.paper.buildChestInterface
import org.incendo.interfaces.paper.PlayerViewer
import org.incendo.interfaces.paper.element.ItemStackElement
import org.incendo.interfaces.paper.pane.ChestPane
import org.koin.core.component.get
import java.awt.SystemColor.menu
import kotlin.time.Duration.Companion.seconds

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
    private val playerArgumentKey = ArgumentKey.of("player", Player::class.java)
    private val opalsArgumentKey = ArgumentKey.of("opals", Int::class.java)
    internal val menus = mutableMapOf<String, Interface<*, PlayerViewer>>()

    override suspend fun load() {
        buildShops()
        particleShop()
        taskAsync {
            do {
                delay(1.seconds)
            } while (!pm.isPluginEnabled(PlayerParticles.getInstance()))

            delay(5.seconds) // I don't know why this is necessary, but it is

            styleShop()
        }
        for ((id, menu) in getConfig().shop.menus) {
            menus.computeIfAbsent(id) { shopMenu(menu) }
        }
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
                .with(opalsArgumentKey, ElixirStorageService.transaction { ElixirPlayer[player.uniqueId].opals })
                .build()
        )

        return true
    }

    private fun buildShops() {
        menus["main"] = buildChestInterface {
            rows = 4
            title = getConfig().shop.title.get()
            clickHandler = ClickHandler.cancel()

            withTransform(0) { view -> view.mask(createMask(rows)) }

            withTransform(1) { view ->
                if (!view.arguments.contains(opalsArgumentKey)) return@withTransform
                view.insertButtons(rows, view.arguments.get(opalsArgumentKey))
            }

            withTransform(3) { view ->
                fun menuButton(
                    x: Int,
                    y: Int,
                    id: String,
                    material: Material,
                    builder: ItemBuilder.() -> Unit
                ) {
                    view[x, y] = ItemBuilderDSL.from(material, builder).asElement { ctx ->
                        if (!ctx.click().leftClick()) return@asElement
                        openShop(ctx.viewer().player(), id)
                    }
                }

                for ((id, menu) in getConfig().shop.menus) {
                    view[menu.position.toVec()] = Items.lookup(menu.display!!).item.asElement { ctx ->
                        if (!ctx.click().leftClick()) return@asElement

                        openShop(ctx.viewer().player(), id)
                    }
                }

                menuButton(4, 1, "particles", Material.BLAZE_POWDER) {
                    name = MiniMessage.miniMessage().deserialize("<gradient:#ff0000:#ff00ff:#0000ff>Particles</gradient>")
                    lore = listOf(MiniMessage.miniMessage().deserialize("<gradient:#ff0000:#ff00ff:#0000ff>Click to open</gradient>"))
                }

                menuButton(5, 1, "pets", Material.BONE) {
                    name = MiniMessage.miniMessage().deserialize("<gradient:#ff0000:#ff00ff:#0000ff>Pets</gradient>")
                    lore = listOf(MiniMessage.miniMessage().deserialize("<gradient:#ff0000:#ff00ff:#0000ff>Click to open</gradient>"))
                }

                menuButton(6, 1, "trails", Material.FEATHER) {
                    name = MiniMessage.miniMessage().deserialize("<gradient:#ff0000:#ff00ff:#0000ff>Trails</gradient>")
                    lore = listOf(MiniMessage.miniMessage().deserialize("<gradient:#ff0000:#ff00ff:#0000ff>Click to open</gradient>"))
                }

                menuButton(2, 1, "mounts", Material.SADDLE) {
                    name = MiniMessage.miniMessage().deserialize("<gradient:#ff0000:#ff00ff:#0000ff>Mounts</gradient>")
                    lore = listOf(MiniMessage.miniMessage().deserialize("<gradient:#ff0000:#ff00ff:#0000ff>Click to open</gradient>"))
                }

                menuButton(3, 1, "stat_tackers", Material.COMPASS) {
                    name = MiniMessage.miniMessage().deserialize("<gradient:#ff0000:#ff00ff:#0000ff>Stat Tackers</gradient>")
                    lore = listOf(MiniMessage.miniMessage().deserialize("<gradient:#ff0000:#ff00ff:#0000ff>Click to open</gradient>"))
                }

                menuButton(4, 2, "styles", Material.SPECTRAL_ARROW) {
                    name = MiniMessage.miniMessage().deserialize("<gradient:#ff0000:#ff00ff:#0000ff>Particle Styles</gradient>")
                    lore = listOf(MiniMessage.miniMessage().deserialize("<gradient:#ff0000:#ff00ff:#0000ff>Click to open</gradient>"))
                }
            }
        }
    }

    private fun getButton(
        button: ElixirConfig.GUI.GUIItemSlot,
        opals: Int?,
        action: GenericClickHandler<ChestPane> = GenericClickHandler.cancel()
    ): ItemStackElement<ChestPane> {
        val item = Items.lookup(button.display)

        return ItemBuilderDSL.from(item.item.clone()) {
            lore = button.lore.map { it["amount" to { opals ?: 0 }] }
        }.asElement(action)
    }

    private fun particleShop(): Interface<*, PlayerViewer> {
        val particles = ParticleEffect.values().filter(ParticleEffect::isEnabled).toMutableSet()
        menus["particles"] = fuckedShop(
            particles,
            { "pp_${it.name}" },
            { effect ->
                ElixirConfig.Modules.Opals.Menu.MenuElement().apply {
                    this.price = 175
                    this.singleUse = true
                    this.command = "lp user <player> permission set playerparticles.effect.${effect.internalName} true"
                    this.display = "${effect.guiIconMaterial.name} name:\"<white>Particle: <gold>${effect.internalName}\""
                }
            }
        )
        return menus["particles"]!!
    }

    private fun styleShop(): Interface<*, PlayerViewer> {
        val styles = PlayerParticles.getInstance().getManager(ParticleStyleManager::class.java).styles
        menus["styles"] = fuckedShop(styles, { "style_${it.name}" }, { style ->
            ElixirConfig.Modules.Opals.Menu.MenuElement().apply {
                this.price = 250
                this.singleUse = true
                this.command = "lp user <player> permission set playerparticles.style.${style.internalName} true"
                this.display = "${style.guiIconMaterial.name} name:\"<white>Style: <gold>${style.internalName}\""
            }
        })

        return menus["styles"]!!
    }

    private fun <T : Any> fuckedShop(
        elements: Collection<T>,
        elementToId: (T) -> String,
        elementToItem: (T) -> ElixirConfig.Modules.Opals.Menu.MenuElement,
        other: MutableChestInterfaceBuilder.() -> Unit = {}
    ) = basePage(elements.count()).apply {
        title = getConfig().shop.title.get()
        val pageTransformer = createReactivatePagination(rows, elements) { element, view ->
            sellableItem(
                view.arguments.get(playerArgumentKey),
                view.arguments.get(opalsArgumentKey),
                elementToId(element),
                elementToItem(element)
            )
        }

        addTransform(pageTransformer, 2)
    }.apply(other).toBuilder().build()

    private fun basePage(elements: Int): MutableChestInterfaceBuilder {
        return MutableChestInterfaceBuilder().apply {
            rows = getPageSize(elements)
            clickHandler = ClickHandler.cancel()

            withTransform(0) { view -> view.mask(createMask(rows)) }
            withTransform(1) { view -> view.insertButtons(rows, view.arguments.get(opalsArgumentKey), "main") }

            withTransform(3) { view -> view.insertButtons(rows, view.arguments.get(opalsArgumentKey), "main") }
            withCloseHandler { _, view ->
                scheduler {
                    openShop(view.arguments.get(playerArgumentKey), "main")
                }.runTaskLater(plugin, 1.ticks)
            }
        }
    }

    private fun <T : Any> createReactivatePagination(
        rows: Int,
        insertedElements: Collection<T>,
        transformation: (T, InterfaceView<ChestPane, PlayerViewer>) -> ItemStackElement<ChestPane>
    ): RenderablePaginatedTransform<ItemStackElement<ChestPane>, ChestPane, PlayerViewer> {
        val buttons = get<ElixirConfig>().guiButtons
        return RenderablePaginatedTransform<ItemStackElement<ChestPane>, ChestPane, PlayerViewer>(
            Vector2.at(1, 1),
            Vector2.at(7, 4)
        ) { insertedElements.map { element -> { view -> transformation(element, view) } } }.apply {
            this.backwardElement(buttons.previousPage.position.toVec(rows)) { transform -> getButton(buttons.previousPage, null) { transform.previousPage() } }
            this.forwardElement(buttons.nextPage.position.toVec(rows)) { transform -> getButton(buttons.nextPage, null) { transform.nextPage() } }
        }
    }

    private fun getPageSize(elements: Int): Int {
        var rows = 2 // Header and footer
        rows += (minOf(elements, 36) / 9) // Allow 1-4 rows of items
        return rows.coerceAtLeast(3) // The above can be 0, so we need to make sure we have at least 3 rows.
    }

    private fun createMask(rows: Int) = buildString {
        append("110000011")
        append("111111111".repeat(maxOf(1, rows - 2)))
        append("110000011")
    }

    private fun shopMenu(menu: ElixirConfig.Modules.Opals.Menu): Interface<*, PlayerViewer> {
        return buildChestInterface {
            rows = maxOf(maxOf(1, menu.elements.size / 9), menu.elements.values.maxOfOrNull { it.position.toVec().x } ?: 0) + 2
            title = getConfig().shop.title.get().append(Component.text(" - ")).append(menu.title!!.get())
            clickHandler = ClickHandler.cancel()

            withTransform(2) { view -> view.mask(createMask(rows)) }

            withTransform(3) { view ->
                if (!view.arguments.contains(playerArgumentKey)) return@withTransform

                val player = view.arguments.get(playerArgumentKey)
                val opals = view.arguments.get(opalsArgumentKey)

                view.insertButtons(rows, opals, "main")

                for ((id, element) in menu.elements) {
                    view[element.position.toVec()] = sellableItem(player, opals, id, element)
                }
            }

            withCloseHandler { _, view ->
                scheduler {
                    openShop(view.arguments.get(playerArgumentKey), "main")
                }.runTaskLater(plugin, 1.ticks)
            }
        }
    }

    private fun sellableItem(
        player: Player,
        opals: Int,
        id: String,
        element: ElixirConfig.Modules.Opals.Menu.MenuElement
    ): ItemStackElement<ChestPane> {
        val staticElement = staticElement(id, element)

        val opalShop = get<ElixirLang>().opalShop
        val opalInfoLore = mutableListOf(
            Component.empty(),
            MiniMessage.miniMessage().parse("<white>Price: <aqua>${element.price}❖"),
            Component.empty()
        )

        when {
            element.price == null -> opalInfoLore += opalShop.itemNotPurchasable.get()
            element.singleUse ?: false && ElixirStorageService.transaction { (ElixirPlayer[player.uniqueId].purchases[id] ?: 0) > 0 } -> opalInfoLore += opalShop.itemAlreadyPurchased.get()
            opals >= element.price!! -> opalInfoLore += opalShop.itemPurchasable.get()
            else -> opalInfoLore += opalShop.itemNotAffordable.map { it["price" to { element.price!!.format() }, "needed" to { (+(element.price!! - opals)).format() }] }
        }

        return ItemBuilderDSL.from(staticElement.itemStack().clone()) {
            this.name = (element.guiName?.get() ?: this.name)?.noItalic()
            this.lore = this.lore + opalInfoLore.map(Component::noItalic)
        }.asElement(staticElement.clickHandler())
    }

    private val staticElements = mutableMapOf<String, ItemStackElement<ChestPane>>()
    private fun staticElement(
        id: String,
        element: ElixirConfig.Modules.Opals.Menu.MenuElement
    ): ItemStackElement<ChestPane> {
        return staticElements.computeIfAbsent(id) { _ ->
            val item = if (element.command.isNullOrBlank()) {
                ItemShopItem(Items.lookup(element.item!!))
            } else CommandShopItem(element.command!!)

            ItemStackElement.of(Items.lookup(element.display!!).item) { ctx ->
                if (!ctx.click().leftClick()) return@of

                val player = ctx.cause().whoClicked.castOrThrow<Player>()
                var opals = 0
                var purchases: Int? = null
                ElixirStorageService.transaction {
                    opals = ElixirPlayer[player.uniqueId].opals
                    purchases = ElixirPlayer[player.uniqueId].purchases[id]
                }

                if (element.singleUse == true && (purchases ?: 0) > 0) {
                    return@of
                }

                if (element.price!! == 0) return@of

                if (opals < element.price!!) {
                    get<ElixirLang>().opalShop.purchaseFailure[
                        "needed" to { (element.price!! - opals).format() },
                        "item" to { Items.lookup(element.display!!).item.displayName() }
                    ] message player

                    player.playSound(Sound.sound(Key.key("entity.villager.no"), Sound.Source.MASTER, 1f, 0.9f))

                    return@of
                }

                ElixirStorageService.transaction {
                    ElixirPlayer[player.uniqueId].opals -= element.price!!
                    if (element.singleUse!!) ElixirPlayer[player.uniqueId].purchases[id] = (purchases ?: 0) + 1
                }

                item.giveTo(player)
                player.playSound(Sound.sound(Key.key("block.note_block.pling"), Sound.Source.MASTER, 1f, 1.5f))

                get<ElixirLang>().opalShop.purchaseSuccess[
                    "item" to { Items.lookup(element.display!!).item.displayName() },
                    "price" to { element.price!!.format() }
                ] message player

                get<ElixirLang>().opalShop.purchaseBroadcast[
                    "player" to { player.displayName() },
                    "item" to { Items.lookup(element.display!!).item.displayName() }
                ] message server

                val sound = Sound.sound(Key.key("entity.player.levelup"), Sound.Source.MASTER, 2f, 1.5f)
                onlinePlayers.forEach { it.playSound(sound) }
            }
        }
    }

    private fun MutableChestPaneView.insertButtons(
        rows: Int,
        opals: Int,
        outerInventory: String? = null
    ) {
        fun placeButton(
            button: ElixirConfig.GUI.GUIItemSlot,
            action: GenericClickHandler<ChestPane>? = null
        ) {
            val item = Items.lookup(button.display)
            if (item is EmptyTestableItem) return logger.debug { "Empty item: ${button.display}" }
            var (col, row) = button.position.toVec()
            row = if (rows < row || row == -1) rows - 1 else row

            this[col, row] = getButton(button, opals, action ?: ClickHandler.cancel())
        }

        val buttons = get<ElixirConfig>().guiButtons

        placeButton(buttons.balance)
        placeButton(buttons.back) { ctx ->
            if (outerInventory != null) {
                openShop(ctx.viewer().player(), outerInventory)
            } else ctx.cause().whoClicked.closeInventory()
        }
    }

    fun Int.format(): String {
        return this.toString().reversed().chunked(3).joinToString(",").reversed()
    }
}

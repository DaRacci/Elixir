package dev.racci.elixir.core.extensions // ktlint-disable filename

import dev.racci.elixir.core.utils.InterfaceUtils
import org.bukkit.Material
import org.incendo.interfaces.core.util.Vector2
import org.incendo.interfaces.kotlin.paper.MutableChestPaneView

fun MutableChestPaneView.mask(
    rawMask: String,
    replacements: List<Material> = InterfaceUtils.defaultReplacements
) = InterfaceUtils.mask(this, rawMask, replacements)

fun String?.toVec(
    rows: Int = -1,
    xOffset: Int = 0,
    yOffset: Int = 1
): Vector2 = InterfaceUtils.toVec(this, rows, xOffset, yOffset)

fun Int.fromIndex(
    rowOffset: Int = 0,
    colOffset: Int = 0
): Vector2 = InterfaceUtils.toVec(this, rowOffset, colOffset)

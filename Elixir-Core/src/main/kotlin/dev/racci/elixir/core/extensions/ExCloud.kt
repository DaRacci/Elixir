package dev.racci.elixir.core.extensions

import cloud.commandframework.bukkit.parsers.PlayerArgument
import cloud.commandframework.context.CommandContext
import cloud.commandframework.kotlin.MutableCommandBuilder
import cloud.commandframework.minecraft.extras.RichDescription
import cloud.commandframework.permission.Permission
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

public fun MutableCommandBuilder<CommandSender>.playerFlag(): MutableCommandBuilder<CommandSender> {
    return flag("player", arrayOf("p"), RichDescription.empty(), PlayerArgument.optional("player"))
}

public fun CommandContext<CommandSender>.targetElseSender(): Player {
    return this.getOrSupplyDefault<Player>("player") {
        if (this.sender is Player) return@getOrSupplyDefault this.sender as Player
        throw IllegalArgumentException("You must specify a player or be a player to use this command!")
    }!!
}

public fun Permission.sub(vararg nodes: String): Permission {
    return Permission.of(this.permission + "." + nodes.joinToString("."))
}

package dev.racci.elixir.core.constants

import cloud.commandframework.permission.CommandPermission
import cloud.commandframework.permission.Permission

enum class ElixirPermission(val permissionString: String) {
    CONNECTION_MESSAGE("elixir.connection-message"),
    CONNECTION_TOGGLE("elixir.connection-message.toggle"),
    CONNECTION_TOGGLE_OTHERS("elixir.connection-message.toggle.others"),
    CONNECTION_MUTATE("elixir.connection-message.customise"),
    CONNECTION_MUTATE_OTHERS("elixir.connection-message.customise.others");

    val permission: CommandPermission = Permission.of(permissionString)
}

package dev.racci.elixir.core.constants

import cloud.commandframework.permission.CommandPermission
import cloud.commandframework.permission.Permission

public enum class ElixirPermission(public val permissionString: String) {
    CONNECTION_MESSAGE("elixir.connection-message"),
    CONNECTION_TOGGLE("elixir.connection-message.toggle"),
    CONNECTION_TOGGLE_OTHERS("elixir.connection-message.toggle.others"),
    CONNECTION_MUTATE("elixir.connection-message.customise"),
    CONNECTION_MUTATE_OTHERS("elixir.connection-message.customise.others"),

    OPALS_MUTATE("elixir.opals.mutate"),
    OPALS_GET("elixir.opals.get"),
    OPALS_SHOP("elixir.opals.shop"),

    RELOAD("elixir.reload"),
    AEHTER_MUTATE("elixir.aether.mutate");

    public val permission: CommandPermission = Permission.of(permissionString)
}

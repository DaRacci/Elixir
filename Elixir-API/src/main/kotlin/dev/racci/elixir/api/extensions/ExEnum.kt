package dev.racci.elixir.api.extensions

public inline fun <reified E : Enum<E>> enumValueOfOrNull(name: String): E? {
    return enumValues<E>().firstOrNull { it.name == name }
}

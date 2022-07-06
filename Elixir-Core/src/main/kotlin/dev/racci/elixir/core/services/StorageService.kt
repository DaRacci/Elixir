package dev.racci.elixir.core.services

import dev.racci.elixir.core.Elixir
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.extension.Extension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.serializer
import kotlin.properties.Delegates

@MappedExtension(Elixir::class, "Storage Service")
@OptIn(ExperimentalSerializationApi::class)
class StorageService(override val plugin: Elixir) : Extension<Elixir>() {

    private val file by lazy { plugin.dataFolder.resolve("config.json") }
    private val json by lazy {
        Json {
            serializersModule = SerializersModule {
                contextual(serializer<String>())
                contextual(serializer<Boolean>())
                contextual(serializer<Int>())
            }
            prettyPrint = true
            encodeDefaults = true
            explicitNulls = true
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    var config by Delegates.notNull<Config>(); private set

    override suspend fun handleLoad() {
        val defaultInput = plugin.getResource("Config.json")!!
        if (!file.exists()) {
            log.debug { "Creating new lang file." }
            withContext(Dispatchers.IO) {
                file.parentFile.mkdirs()
                file.createNewFile()
                file.outputStream().use(defaultInput::copyTo)
                config = json.decodeFromString(defaultInput.readAllBytes().decodeToString())
                defaultInput.close()
            }
            return
        }

        val defaultConfig = json.decodeFromString<Config>(defaultInput.readAllBytes().decodeToString())
        val presentConfig = file.inputStream().use { json.decodeFromString<Config>(it.readAllBytes().decodeToString()) }

        config = if (presentConfig.version != defaultConfig.version) {
            log.info { "Lang file is outdated. Updating from ${presentConfig.version} to ${defaultConfig.version}." }
            for ((key, value) in presentConfig.modules.entries) {
                if (defaultConfig.modules[key] != null) {
                    defaultConfig.modules[key] = value
                } else log.info { "Dropping value as it is no longer used: $key." }
            }
            withContext(Dispatchers.IO) {
                file.outputStream().use { json.encodeToStream(defaultConfig, it) }
            }
            defaultConfig
        } else presentConfig
    }

    inline operator fun <reified T : Any> get(key: String): T = config[key]

    @Serializable
    data class Config(
        val modules: MutableMap<String, MutableMap<String, String>>,
        val version: Int
    ) {
        val valueMap by lazy {
            val map = mutableMapOf<String, String>()
            modules.forEach { (key, value) ->
                value.forEach { (key2, value2) ->
                    map["modules.$key.$key2"] = value2
                }
            }
            println(map.toString())
            map
        }

        inline operator fun <reified T : Any> get(key: String): T {
            val value = valueMap[key] ?: error("No value for key: $key.")
            return when (T::class.java.simpleName) {
                "Boolean" -> value.toBoolean() as T
                "Double" -> value.toDouble() as T
                "Int" -> value.toInt() as T
                "String" -> value as T
                else -> error("Unsupported type: ${T::class.simpleName}.")
            }
        }
    }
    class Generic<T : Any>(private val klass: Class<T>) {
        companion object {
            inline operator fun <reified T : Any> invoke() = Generic(T::class.java)
        }

        fun checkType(t: Any) =
            when {
                klass.isAssignableFrom(t.javaClass) -> true
                else -> false
            }
    }

    companion object : ExtensionCompanion<StorageService>()
}

import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder

// Workaround for (https://youtrack.jetbrains.com/issue/KTIJ-19369)
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.minix.kotlin)
    alias(libs.plugins.minix.copyjar)
    alias(libs.plugins.minix.purpurmc)
    alias(libs.plugins.minecraft.pluginYML)
    alias(libs.plugins.slimjar)
}

bukkit {
    name = project.name
    prefix = project.name
    author = "Racci"
    apiVersion = "1.19"
    version = rootProject.version.toString()
    main = "dev.racci.elixir.core.Elixir"
    load = PluginLoadOrder.POSTWORLD
    depend = listOf("Minix")
    softDepend = listOf(
        "PlaceholderAPI",
        "Lands",
        "ProtocolLib",
        "PlayerParticles",
        "CMI",
        "Terix"
    )
    website = "https://elixir.racci.dev/"
    permissions.create("elixir.connection-message") {
        description = "Broadcasts a message when the player joins or leaves."
        default = Permission.Default.OP
    }
    permissions.create("elixir.connection-message.toggle") {
        description = "Allows the player to toggle their connection message."
        default = Permission.Default.OP
    }
    permissions.create("elixir.connection-message.toggle.others") {
        description = "Allows the player to toggle the connection message of another player."
        default = Permission.Default.OP
    }
    permissions.create("elixir.connection-message.customise") {
        description = "Allows the player to customise their connection message."
        default = Permission.Default.OP
    }
    permissions.create("elixir.connection-message.customise.others") {
        description = "Allows the player to customise the connection message of another player."
        default = Permission.Default.OP
    }
}

dependencies {
    implementation(project(":Elixir-Core"))
    implementation(libs.minecraft.interfaces.paper)
    implementation(libs.minecraft.interfaces.kotlin)
    slim("com.frengor:ultimateadvancementapi-shadeable:2.2.1")
}

tasks {
    shadowJar {
        dependencyFilter.include { dep ->
            dep.moduleGroup == project.group ||
                dep.moduleGroup == libs.minecraft.interfaces.paper.get().module.group
        }
    }

    build {
        finalizedBy(copyJar)
    }
}

allprojects {
    repositories {
        maven("https://nexus.frengor.com/repository/public/") {
            mavenContent {
                releasesOnly()
                includeGroup("com.frengor")
            }
        }
    }
}

subprojects {
    apply<Dev_racci_minix_kotlinPlugin>()
    apply<Dev_racci_minix_purpurmcPlugin>()
    apply<Dev_racci_minix_nmsPlugin>()
    apply(plugin = rootProject.libs.plugins.kotlin.serialization.get().pluginId)

    dependencies {
        compileOnly(rootProject.libs.bundles.kotlin)
        compileOnly(rootProject.libs.bundles.kotlinx)
        compileOnly(rootProject.libs.adventure.kotlin)
        compileOnly(rootProject.libs.koin.core)
        compileOnly(rootProject.libs.minecraft.api.landsAPI)

        compileOnly(rootProject.libs.minecraft.minix)
        compileOnly(rootProject.libs.minecraft.minix.core)
        compileOnly("com.frengor:ultimateadvancementapi-shadeable:2.2.1")
    }

    kotlin.explicitApi()
}

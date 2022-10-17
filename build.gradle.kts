import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission

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
    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.STARTUP
    depend = listOf("Minix")
    softDepend = listOf(
        "PlaceholderAPI",
        "Lands",
        "ProtocolLib"
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
    implementation("org.incendo.interfaces:interfaces-paper:1.0.0-SNAPSHOT")
    implementation("org.incendo.interfaces:interfaces-kotlin:1.0.0-SNAPSHOT")
}

tasks {
    shadowJar {
        dependencyFilter.include {
            it.moduleGroup == "dev.racci" ||
                it.moduleGroup == "org.incendo.interfaces"
        }
    }

    build {
        finalizedBy(copyJar)
    }
}

subprojects {
    apply(plugin = "dev.racci.minix.kotlin")
    apply(plugin = "dev.racci.minix.purpurmc")

    dependencies {
        compileOnly(rootProject.libs.bundles.kotlin)
        compileOnly(rootProject.libs.bundles.kotlinx)
        compileOnly(rootProject.libs.adventure.kotlin)
        compileOnly(rootProject.libs.koin.core)
        compileOnly(rootProject.libs.minecraft.api.landsAPI)

        compileOnly(rootProject.libs.minecraft.minix)
        compileOnly(rootProject.libs.minecraft.minix.core)
    }
}

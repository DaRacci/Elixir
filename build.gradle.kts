plugins {
    id("dev.racci.minix.kotlin")
    id("dev.racci.minix.copyjar")
    id("dev.racci.minix.purpurmc")
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
}

bukkit {
    name = project.name
    prefix = project.name
    author = "Racci"
    apiVersion = "1.18"
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
}

val lib: Configuration by configurations.creating
extensions.getByType<SourceSetContainer>().named(SourceSet.MAIN_SOURCE_SET_NAME) {
    configurations.getByName(compileClasspathConfigurationName).extendsFrom(lib)
    configurations.getByName(runtimeClasspathConfigurationName).extendsFrom(lib)
    configurations.getByName(apiElementsConfigurationName).extendsFrom(lib)
}

dependencies {
    implementation(project(":Elixir-Core"))
    lib(libs.kotlinx.serialization.json)
}

tasks {
    shadowJar {
        dependencyFilter.include {
            it.moduleGroup == "dev.racci"
        }
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
    }
}

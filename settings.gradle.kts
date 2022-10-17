enableFeaturePreview("VERSION_CATALOGS")
pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.racci.dev/releases")
        maven("https://papermc.io/repo/repository/maven-public/")
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://repo.racci.dev/releases")
    }

    val minixConventions: String by settings
    versionCatalogs.create("libs") {
        from("dev.racci:catalog:$minixConventions")
    }
}

include("Elixir-API", "Elixir-Core")

rootProject.name = "Elixir"

plugins {
    kotlin("plugin.serialization")
    id("dev.racci.minix.nms")
}

repositories {
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    implementation(project(":Elixir-API"))
    compileOnly(rootProject.libs.bundles.exposed)
    compileOnly(rootProject.libs.minecraft.commandAPI)
    compileOnly(rootProject.libs.minecraft.minix)
    compileOnly(rootProject.libs.minecraft.api.protoclLib)
    compileOnly(rootProject.libs.caffeine)
    compileOnly(rootProject.libs.bundles.cloud)
    compileOnly(rootProject.libs.bundles.cloud.kotlin)
    compileOnly(files("../lib/CMIAPI8.7.8.2.jar"))

    compileOnly("com.h2database:h2:2.1.214")
}

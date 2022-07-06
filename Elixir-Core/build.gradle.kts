plugins {
    kotlin("plugin.serialization")
    id("dev.racci.minix.nms")
}

repositories {
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    implementation(project(":Elixir-API"))
    compileOnly(rootProject.libs.minecraft.minix)
    compileOnly(rootProject.libs.minecraft.api.protoclLib)
    compileOnly(rootProject.libs.caffeine)
    compileOnly(rootProject.libs.kotlinx.serialization.json)
}

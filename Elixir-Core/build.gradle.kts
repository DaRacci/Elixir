plugins {
    kotlin("plugin.serialization")
    id("dev.racci.minix.nms")
}

repositories {
    mavenLocal()
    maven("https://jitpack.io")
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    implementation(project(":Elixir-API"))
    compileOnly(libs.bundles.exposed)
    compileOnly(libs.minecraft.minix)
    compileOnly(libs.minecraft.api.protoclLib)
    compileOnly(libs.caffeine)
    compileOnly(libs.bundles.cloud)
    compileOnly(libs.bundles.cloud.kotlin)
    compileOnly(libs.minecraft.api.eco)
    compileOnly(files("../lib/CMIAPI8.7.8.2.jar"))
    compileOnly("com.willfp:EcoBosses:8.89.0")
    compileOnly("org.incendo.interfaces:interfaces-paper:1.0.0-SNAPSHOT")
    compileOnly("org.incendo.interfaces:interfaces-kotlin:1.0.0-SNAPSHOT")

    compileOnly("com.h2database:h2:2.1.214")
}

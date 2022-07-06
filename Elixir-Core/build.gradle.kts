plugins {
    kotlin("plugin.serialization")
    id("dev.racci.minix.nms")
}

dependencies {
    implementation(project(":Elixir-API"))
    compileOnly("dev.racci:Minix:3.1.0")
    compileOnly(rootProject.libs.minecraft.api.protoclLib)
    compileOnly(rootProject.libs.caffeine)
    compileOnly(rootProject.libs.kotlinx.serialization.json)
}

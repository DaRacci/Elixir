plugins {
    kotlin("plugin.serialization")
    id("dev.racci.minix.nms")
}

dependencies {
    implementation(project(":Elixir-API"))
    compileOnly("dev.racci:Minix:2.6.2")
    compileOnly(rootProject.libs.minecraft.api.protoclLib)
    compileOnly(rootProject.libs.caffeine)
    compileOnly(rootProject.libs.kotlinx.serialization.json)
}

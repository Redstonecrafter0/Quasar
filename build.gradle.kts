plugins {
    alias(libs.plugins.kotlin)
}

group = "net.redstonecraft.vulkan"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val natives = "natives-${if (System.getProperty("os.name").equals("linux", ignoreCase = true)) "linux" else "windows"}"

dependencies {
    implementation(libs.bundles.lwjgl)
    implementation(libs.joml)
    runtimeOnly(variantOf(libs.lwjgl.core) { classifier(natives) })
    runtimeOnly(variantOf(libs.lwjgl.shaderc) { classifier(natives) })
    runtimeOnly(variantOf(libs.lwjgl.glfw) { classifier(natives) })
    runtimeOnly(variantOf(libs.lwjgl.jemalloc) { classifier(natives) })
    runtimeOnly(variantOf(libs.lwjgl.vma) { classifier(natives) })
}

kotlin {
    jvmToolchain(17)
}

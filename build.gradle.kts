import org.gradle.internal.os.OperatingSystem

plugins {
    alias(libs.plugins.kotlin)
}

group = "net.redstonecraft.vulkan"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val natives = when (OperatingSystem.current()) {
    OperatingSystem.WINDOWS -> "natives-windows"
    OperatingSystem.LINUX -> "natives-linux"
    else -> throw Exception("platform ${OperatingSystem.current().name} not supported")
}

dependencies {
    implementation(libs.bundles.lwjgl)
    implementation(libs.joml)
    runtimeOnly(variantOf(libs.lwjgl.core) { classifier(natives) })
    runtimeOnly(variantOf(libs.lwjgl.glfw) { classifier(natives) })
    runtimeOnly(variantOf(libs.lwjgl.jemalloc) { classifier(natives) })
    runtimeOnly(variantOf(libs.lwjgl.vma) { classifier(natives) })
    runtimeOnly(variantOf(libs.lwjgl.shaderc) { classifier(natives) })
    runtimeOnly(variantOf(libs.lwjgl.spvc) { classifier(natives) })
}

kotlin {
    jvmToolchain(17)
}

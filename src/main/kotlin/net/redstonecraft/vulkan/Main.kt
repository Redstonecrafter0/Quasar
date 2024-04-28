package net.redstonecraft.vulkan

import net.redstonecraft.vulkan.spvc.GLSLCompiler
import net.redstonecraft.vulkan.vfs.ResourceVFS
import net.redstonecraft.vulkan.vk.VulkanContext
import org.lwjgl.glfw.GLFW.*
import kotlin.system.exitProcess

const val width = 1280
const val height = 720

fun main() {

    val compiler = GLSLCompiler(ResourceVFS("/shaders"))

    if (!glfwInit()) {
        exitProcess(-1)
    }
    glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)
    val window = glfwCreateWindow(width, height, "Vulkan Test", 0, 0)

    val vulkan = VulkanContext(window, compiler, "test", "Vulkan Test", Triple(0, 1, 0))

    while (!glfwWindowShouldClose(window)) {
        glfwPollEvents()
    }

    vulkan.close()
    glfwDestroyWindow(window)
    glfwTerminate()
}

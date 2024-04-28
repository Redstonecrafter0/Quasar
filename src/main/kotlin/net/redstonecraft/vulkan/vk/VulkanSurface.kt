package net.redstonecraft.vulkan.vk

import org.lwjgl.glfw.GLFWVulkan.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.VK12.*
import java.io.Closeable

class VulkanSurface(private val instance: VulkanInstance, window: Long): Closeable {

    val surface: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val pSurface = stack.callocLong(1)
            val ret = glfwCreateWindowSurface(instance.instance, window, null, pSurface)
            if (ret != VK_SUCCESS) {
                throw VulkanException("glfwCreateWindowSurface failed", ret)
            }
            surface = pSurface.get(0)
        }
    }

    override fun close() {
        vkDestroySurfaceKHR(instance.instance, surface, null)
    }

}

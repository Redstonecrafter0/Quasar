package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.interfaces.IHandle
import org.lwjgl.glfw.GLFWVulkan.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.VK13.*

class VulkanSurface private constructor(val instance: VulkanInstance, val window: Long): IHandle<Long> {

    override val handle: Long

    class Builder(private val instance: VulkanInstance) {
        var window: Long = 0
        internal fun build() = VulkanSurface(instance, window)
    }

    init {
        MemoryStack.stackPush().use { stack ->
            val pSurface = stack.callocLong(1)
            val ret = glfwCreateWindowSurface(instance.handle, window, null, pSurface)
            if (ret != VK_SUCCESS) {
                throw VulkanException("glfwCreateWindowSurface failed", ret)
            }
            handle = pSurface.get(0)
        }
    }

    override fun close() {
        vkDestroySurfaceKHR(instance.handle, handle, null)
    }

}

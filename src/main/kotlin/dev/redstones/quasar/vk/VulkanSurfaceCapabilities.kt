package dev.redstones.quasar.vk

import dev.redstones.quasar.interfaces.IHandle
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.VkExtent2D
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR

class VulkanSurfaceCapabilities internal constructor(private val physicalDevice: VulkanPhysicalDevice, private val surface: VulkanSurface): IHandle<VkSurfaceCapabilitiesKHR> {

    override lateinit var handle: VkSurfaceCapabilitiesKHR
        private set
    lateinit var extent: VkExtent2D
        private set
    private var manualExtent: Boolean = false

    init {
        create()
    }

    private fun create() {
        MemoryStack.stackPush().use { stack ->
            handle = VkSurfaceCapabilitiesKHR.calloc()
            vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice.handle, surface.handle, handle)

            extent = if (handle.currentExtent().width() != Int.MAX_VALUE && handle.currentExtent().width() != -1) {
                manualExtent = false
                handle.currentExtent()
            } else {
                val width = stack.callocInt(1)
                val height = stack.callocInt(1)
                glfwGetFramebufferSize(surface.window, width, height)
                val actualExtent = VkExtent2D.calloc()
                manualExtent = true
                actualExtent
                    .width(width.get(0).coerceIn(handle.minImageExtent().width(), handle.maxImageExtent().width()))
                    .height(height.get(0).coerceIn(handle.minImageExtent().height(), handle.maxImageExtent().height()))
            }
        }
    }

    fun notifyResize() {
        close()
        create()
    }

    override fun close() {
        if (manualExtent) {
            extent.free()
        }
        handle.free()
    }

}

package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.vk.interfaces.IHandle
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.VkExtent2D
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR

class VulkanSurfaceCapabilities internal constructor(physicalDevice: VulkanPhysicalDevice, surface: VulkanSurface): IHandle<VkSurfaceCapabilitiesKHR> {

    override val handle: VkSurfaceCapabilitiesKHR
    val extent: VkExtent2D
    private val manualExtent: Boolean

    init {
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

    override fun close() {
        if (manualExtent) {
            extent.free()
        }
        handle.free()
    }

}

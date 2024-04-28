package net.redstonecraft.vulkan.vk

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkExtent2D
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR
import org.lwjgl.vulkan.VkSurfaceFormatKHR
import java.io.Closeable

class VulkanPhysicalDeviceSwapChainSupport(private val instance: VulkanInstance, physicalDevice: VulkanPhysicalDevice, surface: VulkanSurface, window: Long): Closeable {

    val capabilities: VkSurfaceCapabilitiesKHR
    val formats: VkSurfaceFormatKHR.Buffer
    val presentModes: List<Int>
    val extent: VkExtent2D

    val preferredPresentMode: Int
        get() {
            if (!instance.vSync && VK_PRESENT_MODE_MAILBOX_KHR in presentModes) {
                return VK_PRESENT_MODE_MAILBOX_KHR
            }
            return VK_PRESENT_MODE_FIFO_KHR // guaranteed to exist
        }

    init {
        MemoryStack.stackPush().use { stack ->
            capabilities = VkSurfaceCapabilitiesKHR.calloc()
            vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice.device, surface.surface, capabilities)

            val formatCount = stack.callocInt(1)
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.device, surface.surface, formatCount, null)
            formats = VkSurfaceFormatKHR.calloc(formatCount.get(0))
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.device, surface.surface, formatCount, formats)

            val presentModeCount = stack.callocInt(1)
            vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice.device, surface.surface, presentModeCount, null)
            val pPresentModes = stack.callocInt(presentModeCount.get(0))
            vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice.device, surface.surface, presentModeCount, pPresentModes)

            presentModes = (0 until pPresentModes.capacity()).map { pPresentModes.get(it) }

            extent = if (capabilities.currentExtent().width() != Int.MAX_VALUE) {
                capabilities.currentExtent()
            } else {
                val width = stack.callocInt(1)
                val height = stack.callocInt(1)
                glfwGetFramebufferSize(window, width, height)
                val actualExtent = VkExtent2D.calloc()
                actualExtent
                    .width(width.get(0).coerceIn(capabilities.minImageExtent().width(), capabilities.maxImageExtent().width()))
                    .height(height.get(0).coerceIn(capabilities.minImageExtent().height(), capabilities.maxImageExtent().height()))
            }
        }
    }

    val preferredFormat: VkSurfaceFormatKHR by lazy {
        for (i in 0 until formats.capacity()) {
            val format = formats.get(i)
            if (format.format() == VK_FORMAT_B8G8R8A8_SRGB && format.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                return@lazy format
            }
        }
        formats.get(0)
    }

    val isValid: Boolean
        get() = formats.capacity() >= 0 && presentModes.isNotEmpty()

    override fun close() {
        memFree(formats)
        capabilities.free()
    }

}

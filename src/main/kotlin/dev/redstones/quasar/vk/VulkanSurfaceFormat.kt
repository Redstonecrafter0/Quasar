package dev.redstones.quasar.vk

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.EXTSwapchainColorspace.*
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkSurfaceFormatKHR

class VulkanSurfaceFormat internal constructor(physicalDevice: VulkanPhysicalDevice, surface: VulkanSurface, hdr: Boolean) {

    val format: Int
    val colorSpace: Int

    init {
        MemoryStack.stackPush().use { stack ->
            val formatCount = stack.callocInt(1)
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.handle, surface.handle, formatCount, null)
            val formats = VkSurfaceFormatKHR.calloc(formatCount.get(0), stack)
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.handle, surface.handle, formatCount, formats)

            if (formats.capacity() == 0) {
                throw VulkanException("No surface formats found")
            }

            if (hdr) {
                for (i in 0 until formats.capacity()) {
                    val format = formats.get(i)
                    if (format.format() == VK_FORMAT_R16G16B16_SFLOAT && format.colorSpace() == VK_COLOR_SPACE_HDR10_HLG_EXT) {
                        this.format = format.format()
                        this.colorSpace = format.colorSpace()
                        return@use
                    }
                }
            }
            for (i in 0 until formats.capacity()) {
                val format = formats.get(i)
                if (format.format() == VK_FORMAT_B8G8R8A8_SRGB && format.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                    this.format = format.format()
                    this.colorSpace = format.colorSpace()
                    return@use
                }
            }
            val format = formats.get(0)
            this.format = format.format()
            this.colorSpace = format.colorSpace()
        }
    }

}

package dev.redstones.quasar.vk

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkQueueFamilyProperties

class VulkanQueueFamilyIndices internal constructor(physicalDevice: VulkanPhysicalDevice, private val surface: VulkanSurface?) {

    var graphicsFamily: Int? = null
        private set
    var presentFamily: Int? = null
        private set
    var computeFamily: Int? = null
        private set

    val isValid: Boolean
        get() = this.graphicsFamily != null && this.computeFamily != null && (surface == null || this.presentFamily != null)

    init {
        MemoryStack.stackPush().use { stack ->
            val queueFamilyCount = stack.callocInt(1)
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice.handle, queueFamilyCount, null)
            val queueFamilies = VkQueueFamilyProperties.calloc(queueFamilyCount.get(0), stack)
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice.handle, queueFamilyCount, queueFamilies)

            for (i in 0 until queueFamilies.capacity()) {
                val cQueueFamily = queueFamilies.get(i)
                val flags = cQueueFamily.queueFlags()
                if (this.graphicsFamily == null && ((flags and VK_QUEUE_GRAPHICS_BIT) != 0)) {
                    this.graphicsFamily = i
                }
                if (this.computeFamily == null && ((flags and VK_QUEUE_COMPUTE_BIT) != 0)) {
                    this.computeFamily = i
                }
                if (surface != null && this.presentFamily == null) {
                    val pPresentSupport = stack.callocInt(1)
                    vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice.handle, i, surface.handle, pPresentSupport)
                    if (pPresentSupport.get(0) != 0) {
                        this.presentFamily = i
                    }
                }
            }
        }
    }

}

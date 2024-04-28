package net.redstonecraft.vulkan.vk

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkQueueFamilyProperties

class VulkanQueueFamilyIndices(device: VulkanPhysicalDevice, surface: VulkanSurface) {

    val graphicsFamily: Int
        get() = _graphicsFamily!!
    val presentFamily: Int
        get() = _presentFamily!!

    val isValid: Boolean
        get() = _graphicsFamily != null && _presentFamily != null

    private var _graphicsFamily: Int? = null
    private var _presentFamily: Int? = null

    init {
        MemoryStack.stackPush().use { stack ->
            val queueFamilyCount = stack.callocInt(1)
            vkGetPhysicalDeviceQueueFamilyProperties(device.device, queueFamilyCount, null)
            val queueFamilies = VkQueueFamilyProperties.calloc(queueFamilyCount.get(0), stack)
            vkGetPhysicalDeviceQueueFamilyProperties(device.device, queueFamilyCount, queueFamilies)

            for (i in 0 until queueFamilies.capacity()) {
                val cQueueFamily = queueFamilies.get(i)
                val flags = cQueueFamily.queueFlags()
                if (_graphicsFamily == null && ((flags and VK_QUEUE_GRAPHICS_BIT) != 0)) {
                    _graphicsFamily = i
                }
                if (_presentFamily == null) {
                    val pPresentSupport = stack.callocInt(1)
                    vkGetPhysicalDeviceSurfaceSupportKHR(device.device, i, surface.surface, pPresentSupport)
                    if (pPresentSupport.get(0) != 0) {
                        _presentFamily = i
                    }
                }
            }
        }
    }

}

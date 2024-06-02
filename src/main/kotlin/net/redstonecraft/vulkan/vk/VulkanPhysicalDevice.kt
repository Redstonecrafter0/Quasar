package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.vk.interfaces.IHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkExtensionProperties
import org.lwjgl.vulkan.VkPhysicalDevice
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
import org.lwjgl.vulkan.VkPhysicalDeviceProperties
import java.nio.ByteBuffer

class VulkanPhysicalDevice internal constructor(val instance: VulkanInstance, val surface: VulkanSurface?, deviceHandle: Long, val extensions: List<String>): IHandle<VkPhysicalDevice> {

    override val handle = VkPhysicalDevice(deviceHandle, instance.handle)
    val queueFamilyIndices = VulkanQueueFamilyIndices(this, surface)
    val surfaceCapabilities = if (surface != null) VulkanSurfaceCapabilities(this, surface) else null
    val surfaceFormat = if (surface != null) VulkanSurfaceFormat(this, surface, instance.hdr) else null
    val presentMode: Int?
    val isDiscrete: Boolean
    val deviceName: String
    val memory: Long

    private val extensionsAvailable: Boolean

    init {
        MemoryStack.stackPush().use { stack ->
            extensionsAvailable = checkExtensions(stack)
            presentMode = getPresentMode(stack, surface, instance)
            val properties = getProperties(stack)
            isDiscrete = properties.first
            deviceName = properties.second
            memory = getMemory(stack)
        }
    }

    private fun getPresentMode(stack: MemoryStack, surface: VulkanSurface?, instance: VulkanInstance): Int? {
        if (surface == null) {
            return null
        }
        val presentModeCount = stack.callocInt(1)
        vkGetPhysicalDeviceSurfacePresentModesKHR(handle, surface.handle, presentModeCount, null)
        val pPresentModes = stack.callocInt(presentModeCount.get(0))
        vkGetPhysicalDeviceSurfacePresentModesKHR(handle, surface.handle, presentModeCount, pPresentModes)

        val presentModes = (0 until pPresentModes.capacity()).map { pPresentModes.get(it) }

        if (presentModes.isEmpty()) {
            throw VulkanException("No present modes available") // should never happen
        }

        if (!instance.vSync && VK_PRESENT_MODE_IMMEDIATE_KHR in presentModes) {
            return VK_PRESENT_MODE_IMMEDIATE_KHR
        }
        return VK_PRESENT_MODE_FIFO_KHR // guaranteed to exist
    }

    private fun getMemory(stack: MemoryStack): Long {
        val memoryProperties = VkPhysicalDeviceMemoryProperties.calloc(stack)
        vkGetPhysicalDeviceMemoryProperties(handle, memoryProperties)

        var memory = 0L
        for (j in 0 until memoryProperties.memoryHeapCount()) {
            if ((memoryProperties.memoryHeaps(j).flags() and VK_MEMORY_HEAP_DEVICE_LOCAL_BIT) != 0) { // dedicated memory
                memory = memoryProperties.memoryHeaps(j).size()
                break
            }
            val cMemory = memoryProperties.memoryHeaps(j).size()
            if (cMemory > memory) {
                memory = cMemory
            }
        }
        return memory
    }

    private fun getProperties(stack: MemoryStack): Pair<Boolean, String> {
        val properties = VkPhysicalDeviceProperties.calloc(stack)
        vkGetPhysicalDeviceProperties(handle, properties)
        return (properties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) to properties.deviceNameString()
    }

    private fun checkExtensions(stack: MemoryStack): Boolean {
        val extensionCount = stack.callocInt(1)
        vkEnumerateDeviceExtensionProperties(handle, null as ByteBuffer?, extensionCount, null)
        val availableExtensions = VkExtensionProperties.calloc(extensionCount.get(0), stack)
        vkEnumerateDeviceExtensionProperties(handle, null as ByteBuffer?, extensionCount, availableExtensions)

        val requiredExtensions = extensions.toMutableList()
        availableExtensions.forEach {
            requiredExtensions -= it.extensionNameString()
        }
        return requiredExtensions.isEmpty()
    }

    fun buildLogicalDevice() = VulkanLogicalDevice(this)

    val isValid: Boolean
        get() = queueFamilyIndices.isValid && extensionsAvailable

    override fun close() {
        surfaceCapabilities?.close()
    }

}

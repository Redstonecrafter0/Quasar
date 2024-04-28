package net.redstonecraft.vulkan.vk

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkExtensionProperties
import org.lwjgl.vulkan.VkPhysicalDevice
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
import org.lwjgl.vulkan.VkPhysicalDeviceProperties
import java.io.Closeable
import java.nio.ByteBuffer

class VulkanPhysicalDevice(instance: VulkanInstance, surface: VulkanSurface, window: Long, handle: Long): Closeable {

    val device = VkPhysicalDevice(handle, instance.instance)
    val queueFamilyIndices = VulkanQueueFamilyIndices(this, surface)
    val swapChainSupport = VulkanPhysicalDeviceSwapChainSupport(instance, this, surface, window)
    val discrete: Boolean
    val deviceName: String
    val memory: Long

    private val extensionsAvailable: Boolean

    init {
        MemoryStack.stackPush().use { stack ->
            extensionsAvailable = checkExtensions(stack, instance)
            val properties = getProperties(stack)
            discrete = properties.first
            deviceName = properties.second
            memory = getMemory(stack)
        }
    }

    private fun getMemory(stack: MemoryStack): Long {
        val memoryProperties = VkPhysicalDeviceMemoryProperties.calloc(stack)
        vkGetPhysicalDeviceMemoryProperties(device, memoryProperties)

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
        vkGetPhysicalDeviceProperties(device, properties)
        return (properties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) to properties.deviceNameString()
    }

    private fun checkExtensions(stack: MemoryStack, instance: VulkanInstance): Boolean {
        val extensionCount = stack.callocInt(1)
        vkEnumerateDeviceExtensionProperties(device, null as ByteBuffer?, extensionCount, null)
        val availableExtensions = VkExtensionProperties.calloc(extensionCount.get(0), stack)
        vkEnumerateDeviceExtensionProperties(device, null as ByteBuffer?, extensionCount, availableExtensions)

        val requiredExtensions = instance.extensions.toMutableList()
        availableExtensions.forEach {
            requiredExtensions -= it.extensionNameString()
        }
        return requiredExtensions.isEmpty()
    }

    val isValid: Boolean
        get() = queueFamilyIndices.isValid && extensionsAvailable && swapChainSupport.isValid

    override fun close() {
        swapChainSupport.close()
    }

}

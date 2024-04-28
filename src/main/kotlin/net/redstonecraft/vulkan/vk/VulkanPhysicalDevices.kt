package net.redstonecraft.vulkan.vk

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK12.*
import java.io.Closeable

class VulkanPhysicalDevices(instance: VulkanInstance, surface: VulkanSurface, window: Long): Closeable {

    val devices: List<VulkanPhysicalDevice>

    init {
        MemoryStack.stackPush().use { stack ->
            val deviceCount = stack.callocInt(1)
            vkEnumeratePhysicalDevices(instance.instance, deviceCount, null)
            val pDevices = stack.callocPointer(deviceCount.get(0))
            vkEnumeratePhysicalDevices(instance.instance, deviceCount, pDevices)

            devices = (0 until deviceCount.get(0)).map {
                VulkanPhysicalDevice(instance, surface, window, pDevices.get(it))
            }.filter { it.isValid }
        }
    }

    fun pickDevice(): VulkanPhysicalDevice {
        return devices
            .groupBy { it.discrete }
            .mapValues { it.value.sortedByDescending { i -> i.memory } }
            .asSequence()
            .sortedBy { if (it.key) 0 else 1 }
            .map { it.value }
            .flatten()
            .first()
    }

    override fun close() {
        devices.forEach { it.close() }
    }

}
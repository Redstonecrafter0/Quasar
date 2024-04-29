package net.redstonecraft.vulkan.vk

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*
import java.io.Closeable

class VulkanLogicalDevice(instance: VulkanInstance, val physicalDevice: VulkanPhysicalDevice): Closeable {

    val device: VkDevice
    val queues: List<VkQueue>

    init {
        MemoryStack.stackPush().use { stack ->
            val queueFamilyList = listOf(physicalDevice.queueFamilyIndices.graphicsFamily, physicalDevice.queueFamilyIndices.presentFamily)
            val queueFamilySet = queueFamilyList.toSet()
            val queueCreateInfos = buildQueueCreateInfo(queueFamilySet, stack)
            val pExtensionNames = buildExtensionNames(stack, instance.extensions)
            val deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack)
            val createInfo = VkDeviceCreateInfo.calloc(stack).`sType$Default`()
                .pQueueCreateInfos(queueCreateInfos)
                .pEnabledFeatures(deviceFeatures)
                .ppEnabledExtensionNames(pExtensionNames)
            val pDevice = stack.callocPointer(1)
            val ret = vkCreateDevice(physicalDevice.device, createInfo, null, pDevice)
            if (ret != VK_SUCCESS) {
                throw VulkanException("vkCreateDevice failed", ret)
            }
            device = VkDevice(pDevice.get(0), physicalDevice.device, createInfo)
            queues = queueFamilySet.map {
                val pQueue = stack.callocPointer(1)
                vkGetDeviceQueue(device, it, 0, pQueue)
                VkQueue(pQueue.get(0), device)
            }
        }
    }

    private fun buildQueueCreateInfo(queueFamilySet: Set<Int>, stack: MemoryStack): VkDeviceQueueCreateInfo.Buffer {
        val queueCreateInfos = VkDeviceQueueCreateInfo.calloc(queueFamilySet.size, stack)
        val queuePriority = stack.callocFloat(1).put(1F).flip()
        queueFamilySet.forEach {
            queueCreateInfos.put(
                VkDeviceQueueCreateInfo.calloc(stack).`sType$Default`()
                    .queueFamilyIndex(it)
                    .pQueuePriorities(queuePriority)
            )
        }
        queueCreateInfos.flip()
        return queueCreateInfos
    }

    private fun buildExtensionNames(stack: MemoryStack, extensions: List<String>): PointerBuffer {
        val pExtensionNames = stack.callocPointer(extensions.size)
        for (i in extensions) {
            val extensionName = stack.UTF8(i)
            pExtensionNames.put(memAddress(extensionName))
        }
        pExtensionNames.flip()
        return pExtensionNames
    }

    override fun close() {
        vkDestroyDevice(device, null)
    }

}

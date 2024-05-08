package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.vk.interfaces.IHandle
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*

class VulkanLogicalDevice internal constructor(val physicalDevice: VulkanPhysicalDevice): IHandle<VkDevice> {

    override val handle: VkDevice
    val queues: List<VkQueue>

    init {
        MemoryStack.stackPush().use { stack ->
            val queueFamilySet = setOfNotNull(
                physicalDevice.queueFamilyIndices.graphicsFamily,
                physicalDevice.queueFamilyIndices.presentFamily
            )
            val queueCreateInfos = buildQueueCreateInfo(queueFamilySet, stack)
            val pExtensionNames = buildExtensionNames(stack, physicalDevice.extensions)
            val deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack)
            val createInfo = VkDeviceCreateInfo.calloc(stack).`sType$Default`()
                .pQueueCreateInfos(queueCreateInfos)
                .pEnabledFeatures(deviceFeatures)
                .ppEnabledExtensionNames(pExtensionNames)
            val pDevice = stack.callocPointer(1)
            val ret = vkCreateDevice(physicalDevice.handle, createInfo, null, pDevice)
            if (ret != VK_SUCCESS) {
                throw VulkanException("vkCreateDevice failed", ret)
            }
            handle = VkDevice(pDevice.get(0), physicalDevice.handle, createInfo)
            queues = queueFamilySet.map {
                val pQueue = stack.callocPointer(1)
                vkGetDeviceQueue(handle, it, 0, pQueue)
                VkQueue(pQueue.get(0), handle)
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

    fun buildSwapChain(block: VulkanSwapChain.Builder.() -> Unit): VulkanSwapChain {
        val builder = VulkanSwapChain.Builder(this)
        builder.block()
        return builder.build()
    }

    fun buildImageView(image: Long, format: Int) = VulkanImageView.Builder(this, image, format)

    override fun close() {
        vkDestroyDevice(handle, null)
    }

}

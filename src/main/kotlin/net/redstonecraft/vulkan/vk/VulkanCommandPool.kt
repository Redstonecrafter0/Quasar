package net.redstonecraft.vulkan.vk

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkCommandPoolCreateInfo
import java.io.Closeable

class VulkanCommandPool(val device: VulkanLogicalDevice): Closeable {

    val commandPool: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val poolInfo = VkCommandPoolCreateInfo.calloc(stack).`sType$Default`()
                .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                .queueFamilyIndex(device.physicalDevice.queueFamilyIndices.graphicsFamily)
            val pCommandPool = stack.callocLong(1)
            val ret = vkCreateCommandPool(device.device, poolInfo, null, pCommandPool)
            if (ret != VK_SUCCESS) {
                throw VulkanException("vkCreateCommandPool failed", ret)
            }
            commandPool = pCommandPool.get(0)
        }
    }

    override fun close() {
        vkDestroyCommandPool(device.device, commandPool, null)
    }

}

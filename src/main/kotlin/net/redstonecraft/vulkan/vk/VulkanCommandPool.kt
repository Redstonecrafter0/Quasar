package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.vk.interfaces.IHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkCommandPoolCreateInfo

class VulkanCommandPool private constructor(val device: VulkanLogicalDevice, flags: Int): IHandle<Long> {

    class Builder internal constructor(private val device: VulkanLogicalDevice) {
        var flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT

        internal fun build() = VulkanCommandPool(device, flags)
    }

    override val handle: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val poolInfo = VkCommandPoolCreateInfo.calloc(stack).`sType$Default`()
                .flags(flags)
                .queueFamilyIndex(device.physicalDevice.queueFamilyIndices.graphicsFamily!!)
            val pCommandPool = stack.callocLong(1)
            val ret = vkCreateCommandPool(device.handle, poolInfo, null, pCommandPool)
            if (ret != VK_SUCCESS) {
                throw VulkanException("vkCreateCommandPool failed", ret)
            }
            handle = pCommandPool.get(0)
        }
    }

    override fun close() {
        vkDestroyCommandPool(device.handle, handle, null)
    }

}

package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.interfaces.IHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkSemaphoreCreateInfo

class VulkanSemaphore internal constructor(val device: VulkanLogicalDevice): IHandle<Long> {

    override val handle: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack).`sType$Default`()
            val pSemaphore = stack.callocLong(1)
            val ret = vkCreateSemaphore(device.handle, semaphoreInfo, null, pSemaphore)
            if (ret != VK_SUCCESS) {
                throw VulkanException("vkCreateSemaphore failed", ret)
            }
            handle = pSemaphore.get(0)
        }
    }

    override fun close() {
        vkDestroySemaphore(device.handle, handle, null)
    }

}

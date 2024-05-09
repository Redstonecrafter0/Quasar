package net.redstonecraft.vulkan.vk

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo

class VulkanCommandBuffer(commandPool: VulkanCommandPool, device: VulkanLogicalDevice) {

    val commandBuffer: VkCommandBuffer

    init {
        MemoryStack.stackPush().use { stack ->
            val allocInfo = VkCommandBufferAllocateInfo.calloc(stack).`sType$Default`()
                .commandPool(commandPool.handle)
                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandBufferCount(1)
            val pCommandBuffer = stack.callocPointer(1)
            val ret = vkAllocateCommandBuffers(device.handle, allocInfo, pCommandBuffer)
            if (ret != VK_SUCCESS) {
                throw VulkanException("vkAllocateCommandBuffers failed", ret)
            }
            commandBuffer = VkCommandBuffer(pCommandBuffer.get(0), device.handle)
        }
    }

}

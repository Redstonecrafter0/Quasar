package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.vk.interfaces.IHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo
import org.lwjgl.vulkan.VkCommandBufferBeginInfo

class VulkanCommandBuffer private constructor(val commandPool: VulkanCommandPool): IHandle<VkCommandBuffer> {

    class Builder internal constructor(private val commandPool: VulkanCommandPool) {

        fun build() = VulkanCommandBuffer(commandPool)
    }

    override val handle: VkCommandBuffer

    init {
        MemoryStack.stackPush().use { stack ->
            val allocInfo = VkCommandBufferAllocateInfo.calloc(stack).`sType$Default`()
                .commandPool(commandPool.handle)
                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandBufferCount(1)
            val pCommandBuffer = stack.callocPointer(1)
            val ret = vkAllocateCommandBuffers(commandPool.device.handle, allocInfo, pCommandBuffer)
            if (ret != VK_SUCCESS) {
                throw VulkanException("vkAllocateCommandBuffers failed", ret)
            }
            handle = VkCommandBuffer(pCommandBuffer.get(0), commandPool.device.handle)
        }
    }

    fun begin(): VulkanCommandBufferRecorder {
        MemoryStack.stackPush().use { stack ->
            val beginInfo = VkCommandBufferBeginInfo.calloc(stack).`sType$Default`()
                .flags(0)
                .pInheritanceInfo(null)
            val ret = vkBeginCommandBuffer(handle, beginInfo)
            if (ret != VK_SUCCESS) {
                throw VulkanException("vkBeginCommandBuffer failed", ret)
            }
        }
        return VulkanCommandBufferRecorder(this)
    }

    override fun close() {
        // implicitly closed when parent command pool is closed
    }

}

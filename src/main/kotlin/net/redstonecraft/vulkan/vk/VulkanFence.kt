package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.vk.interfaces.IHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkFenceCreateInfo

class VulkanFence private constructor(val device: VulkanLogicalDevice, flags: Int): IHandle<Long> {

    class Builder internal constructor(private val device: VulkanLogicalDevice) {

        var isSignalled = true

        fun build() = VulkanFence(device, if (isSignalled) VK_FENCE_CREATE_SIGNALED_BIT else 0)
    }

    override val handle: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val fenceInfo = VkFenceCreateInfo.calloc(stack).`sType$Default`()
                .flags(flags)
            val pFence = stack.callocLong(1)
            val ret = vkCreateFence(device.handle, fenceInfo, null, pFence)
            if (ret != VK_SUCCESS) {
                throw VulkanException("vkCreateFence failed", ret)
            }
            handle = pFence.get(0)
        }
    }

    fun waitForFence(timeout: Long = Long.MAX_VALUE) {
        vkWaitForFences(device.handle, handle, true, timeout)
    }

    fun reset() {
        vkResetFences(device.handle, handle)
    }

    override fun close() {
        vkDestroyFence(device.handle, handle, null)
    }

}

fun List<VulkanFence>.waitForAll(waitForAll: Boolean = true, timeout: Long = Long.MAX_VALUE) {
    groupBy { it.device }.forEach { (device, list) ->
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.callocLong(size)
            list.forEach { buffer.put(it.handle) }
            buffer.flip()
            vkWaitForFences(device.handle, buffer, waitForAll, timeout)
        }
    }
}

fun List<VulkanFence>.resetAll() {
    groupBy { it.device }.forEach { (device, list) ->
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.callocLong(size)
            list.forEach { buffer.put(it.handle) }
            buffer.flip()
            vkResetFences(device.handle, buffer)
        }
    }
}

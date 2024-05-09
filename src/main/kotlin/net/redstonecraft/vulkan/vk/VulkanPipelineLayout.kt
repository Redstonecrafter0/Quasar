package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.vk.interfaces.IHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*

class VulkanPipelineLayout(
    val device: VulkanLogicalDevice
): IHandle<Long> {

    override val handle: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack).`sType$Default`()
                .setLayoutCount(0)
                .pSetLayouts(null)
                .pPushConstantRanges(null)
            val pPipelineLayout = stack.callocLong(1)
            val ret = vkCreatePipelineLayout(device.handle, pipelineLayoutInfo, null, pPipelineLayout)
            if (ret != VK_SUCCESS) {
                throw VulkanException("vkCreatePipelineLayout failed", ret)
            }
            handle = pPipelineLayout.get(0)
        }
    }

    override fun close() {
        vkDestroyPipelineLayout(device.handle, handle, null)
    }

}

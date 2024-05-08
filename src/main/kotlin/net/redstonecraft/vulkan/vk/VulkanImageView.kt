package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.vk.interfaces.IHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkComponentMapping
import org.lwjgl.vulkan.VkImageSubresourceRange
import org.lwjgl.vulkan.VkImageViewCreateInfo

class VulkanImageView internal constructor(val device: VulkanLogicalDevice, image: Long, val format: Int): IHandle<Long> {

    class Builder internal constructor(private val device: VulkanLogicalDevice, private val image: Long, private val format: Int) {

        fun build() = VulkanImageView(device, image, format)
    }

    override val handle: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val componentMapping = VkComponentMapping.calloc(stack)
                .r(VK_COMPONENT_SWIZZLE_IDENTITY)
                .g(VK_COMPONENT_SWIZZLE_IDENTITY)
                .b(VK_COMPONENT_SWIZZLE_IDENTITY)
                .a(VK_COMPONENT_SWIZZLE_IDENTITY)
            val subResourceRange = VkImageSubresourceRange.calloc(stack)
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1)
            val createInfo = VkImageViewCreateInfo.calloc(stack).`sType$Default`()
                .image(image)
                .viewType(VK_IMAGE_VIEW_TYPE_2D)
                .format(format)
                .components(componentMapping)
                .subresourceRange(subResourceRange)
            val pImageView = stack.callocLong(1)
            val ret = vkCreateImageView(device.handle, createInfo, null, pImageView)
            if (ret != VK_SUCCESS) {
                throw VulkanException("vkCreateImageView failed", ret)
            }
            handle = pImageView.get(0)
        }
    }

    override fun close() {
        vkDestroyImageView(device.handle, handle, null)
    }

}

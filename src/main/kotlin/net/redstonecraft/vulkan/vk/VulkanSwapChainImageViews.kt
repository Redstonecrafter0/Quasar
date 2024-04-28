package net.redstonecraft.vulkan.vk

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkComponentMapping
import org.lwjgl.vulkan.VkImageSubresourceRange
import org.lwjgl.vulkan.VkImageViewCreateInfo
import java.io.Closeable
import java.nio.LongBuffer

class VulkanSwapChainImageViews(swapChain: VulkanSwapChain, private val device: VulkanLogicalDevice): Closeable {

    val swapChainImageViews = mutableListOf<Long>()

    init {
        MemoryStack.stackPush().use { stack ->
            for (i in 0 until swapChain.swapChainImages.capacity()) {
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
                    .image(swapChain.swapChainImages.get(i))
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(swapChain.swapChainImageFormat.format())
                    .components(componentMapping)
                    .subresourceRange(subResourceRange)
                val swapChainImageView = stack.callocLong(1)
                val ret = vkCreateImageView(device.device, createInfo, null, swapChainImageView)
                if (ret != VK_SUCCESS) {
                    throw VulkanException("vkCreateImageView failed", ret)
                }
                swapChainImageViews += swapChainImageView.get(0)
            }
        }
    }

    override fun close() {
        for (i in swapChainImageViews) {
            vkDestroyImageView(device.device, i, null)
        }
    }

}

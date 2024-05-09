package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.vk.interfaces.IHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkExtent2D
import org.lwjgl.vulkan.VkFramebufferCreateInfo

class VulkanFramebuffer(val device: VulkanLogicalDevice, extent: VkExtent2D, imageView: VulkanImageView, renderPass: VulkanRenderPass): IHandle<Long> {

    class Builder internal constructor(private val device: VulkanLogicalDevice) {

        var extent: VkExtent2D? = null
        var imageView: VulkanImageView? = null
        var renderPass: VulkanRenderPass? = null

        fun build(): VulkanFramebuffer {
            requireNotNull(extent) { "extent must be not null" }
            requireNotNull(imageView) { "imageView must be not null" }
            requireNotNull(renderPass) { "renderPass must be not null" }
            return VulkanFramebuffer(device, extent!!, imageView!!, renderPass!!)
        }
    }

    override val handle: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val attachments = stack.callocLong(1)
                .put(imageView.handle)
                .flip()
            val framebufferInfo = VkFramebufferCreateInfo.calloc(stack).`sType$Default`()
                .renderPass(renderPass.handle)
                .attachmentCount(attachments.capacity())
                .pAttachments(attachments)
//                .width(swapChain.device.physicalDevice.surfaceCapabilities!!.extent.width())
//                .height(swapChain.device.physicalDevice.surfaceCapabilities.extent.height())
                .width(extent.width())
                .height(extent.height())
                .layers(1)
            val pFramebuffer = stack.callocLong(1)
            val ret = vkCreateFramebuffer(device.handle, framebufferInfo, null, pFramebuffer)
            if (ret != VK_SUCCESS) {
                throw VulkanException("vkCreateFramebuffer failed", ret)
            }
            handle = pFramebuffer.get(0)
        }
    }

    override fun close() {
        vkDestroyFramebuffer(device.handle, handle, null)
    }

}

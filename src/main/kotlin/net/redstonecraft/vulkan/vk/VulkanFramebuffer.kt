package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.vk.interfaces.IHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkFramebufferCreateInfo

class VulkanFramebuffer(val device: VulkanLogicalDevice, swapChain: VulkanSwapChain, imageView: VulkanImageView, renderPass: VulkanRenderPass): IHandle<Long> {

    override val handle: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val attachments = stack.callocLong(1)
                .put(imageView.handle)
                .flip()
            val framebufferInfo = VkFramebufferCreateInfo.calloc(stack).`sType$Default`()
                .renderPass(renderPass.renderPass)
                .attachmentCount(attachments.capacity())
                .pAttachments(attachments)
                .width(swapChain.device.physicalDevice.surfaceCapabilities!!.extent.width())
                .height(swapChain.device.physicalDevice.surfaceCapabilities.extent.height())
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

package net.redstonecraft.vulkan.vk

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkFramebufferCreateInfo
import java.io.Closeable

class VulkanFramebuffers(val device: VulkanLogicalDevice, swapChain: VulkanSwapChain, swapChainImageViews: VulkanImageView, renderPass: VulkanRenderPass): Closeable {

    val framebuffers = mutableListOf<Long>()

    init {
        MemoryStack.stackPush().use { stack ->
            for (i in swapChainImageViews.swapChainImageViews) {
                val attachments = stack.callocLong(1)
                    .put(i)
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
                framebuffers += pFramebuffer.get(0)
            }
        }
    }

    override fun close() {
        framebuffers.forEach { vkDestroyFramebuffer(device.handle, it, null) }
    }

}

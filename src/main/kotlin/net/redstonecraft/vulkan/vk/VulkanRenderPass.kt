package net.redstonecraft.vulkan.vk

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK12.*
import java.io.Closeable

class VulkanRenderPass(val device: VulkanLogicalDevice, val swapChain: VulkanSwapChain): Closeable {

    val renderPass: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val colorAttachment = VkAttachmentDescription.calloc(stack)
                .format(swapChain.swapChainImageFormat.format())
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
            val colorAttachments = VkAttachmentDescription.calloc(1, stack)
                .put(colorAttachment)
                .flip()
            val colorAttachmentRef = VkAttachmentReference.calloc(stack)
                .attachment(0)
                .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
            val colorAttachmentRefs = VkAttachmentReference.calloc(1, stack)
                .put(colorAttachmentRef)
                .flip()
            val subPass = VkSubpassDescription.calloc(stack)
                .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                .colorAttachmentCount(colorAttachmentRefs.capacity())
                .pColorAttachments(colorAttachmentRefs)
            val subPasses = VkSubpassDescription.calloc(1, stack)
                .put(subPass)
                .flip()
            val renderPassInfo = VkRenderPassCreateInfo.calloc(stack).`sType$Default`()
                .pAttachments(colorAttachments)
                .pSubpasses(subPasses)
            val pRenderPass = stack.callocLong(1)
            val ret = vkCreateRenderPass(device.device, renderPassInfo, null, pRenderPass)
            if (ret != VK_SUCCESS) {
                throw VulkanException("vkCreateRenderPass failed", ret)
            }
            renderPass = pRenderPass.get(0)
        }
    }

    override fun close() {
        vkDestroyRenderPass(device.device, renderPass, null)
    }

}

package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.vk.data.SubpassInfo
import net.redstonecraft.vulkan.vk.enums.VulkanAttachmentLoadOp
import net.redstonecraft.vulkan.vk.interfaces.IHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK13.*

class VulkanRenderPass private constructor(
    val device: VulkanLogicalDevice,
    attachments: List<VkAttachmentDescription>,
    graphicsPipelineBuilderBlocks: List<VulkanGraphicsPipeline.Builder.() -> Unit>
): IHandle<Long> {

    class Builder internal constructor(private val device: VulkanLogicalDevice) {

        inner class AttachmentBuilder internal constructor(val format: Int) {

            var msaa = VK_SAMPLE_COUNT_1_BIT
            var loadOp = VulkanAttachmentLoadOp.CLEAR

            internal fun build(): VkAttachmentDescription {
                return VkAttachmentDescription.calloc(stack)
                    .format(format)
                    .samples(msaa)
                    .loadOp(loadOp.loadOp)
                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            }
        }

        private val stack = MemoryStack.stackPush()

        private val attachments = mutableListOf<VkAttachmentDescription>()
        private val graphicsPipelineBuilderBlocks = mutableListOf<VulkanGraphicsPipeline.Builder.() -> Unit>()

        fun colorAttachment(format: Int, block: AttachmentBuilder.() -> Unit): Int {
            val builder = AttachmentBuilder(format)
            builder.block()
            attachments += builder.build().finalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
            return attachments.size - 1
        }

        fun presentColorAttachment(format: Int, block: AttachmentBuilder.() -> Unit): Int {
            val builder = AttachmentBuilder(format)
            builder.block()
            attachments += builder.build().finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
            return attachments.size - 1
        }

        fun graphicsSubpass(block: VulkanGraphicsPipeline.Builder.() -> Unit): Int {
            graphicsPipelineBuilderBlocks += block
            return graphicsPipelineBuilderBlocks.size - 1
        }

        internal fun build(): VulkanRenderPass {
            return stack.use {
                VulkanRenderPass(device, attachments, graphicsPipelineBuilderBlocks)
            }
        }
    }

    override val handle: Long

    internal val subpasses: List<SubpassInfo>

    init {
        MemoryStack.stackPush().use { stack ->
            val graphicsPipelineBuilders = graphicsPipelineBuilderBlocks.map {
                VulkanGraphicsPipeline.Builder(this).apply(it)
            }
            val pAttachments = VkAttachmentDescription.calloc(attachments.size, stack)
            for (i in attachments) {
                pAttachments.put(i)
            }
            pAttachments.flip()
            val pSubPasses = VkSubpassDescription.calloc(graphicsPipelineBuilders.size, stack)
            for (i in graphicsPipelineBuilders) {
                val colorAttachmentRefs = VkAttachmentReference.calloc(i.attachmentRefs.size, stack)
                for ((attachment, layout) in i.attachmentRefs) {
                    colorAttachmentRefs.put(VkAttachmentReference.calloc(stack)
                        .attachment(attachment)
                        .layout(layout)
                    )
                }
                colorAttachmentRefs.flip()
                val subPass = VkSubpassDescription.calloc(stack)
                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .colorAttachmentCount(colorAttachmentRefs.capacity())
                    .pColorAttachments(colorAttachmentRefs)
                pSubPasses.put(subPass)
            }
            pSubPasses.flip()
            val dependency = VkSubpassDependency.calloc(stack)
                .srcSubpass(VK_SUBPASS_EXTERNAL)
                .dstSubpass(0)
                .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .srcAccessMask(0)
                .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
            val dependencies = VkSubpassDependency.calloc(1, stack)
                .put(dependency)
                .flip()
            val renderPassInfo = VkRenderPassCreateInfo.calloc(stack).`sType$Default`()
                .pAttachments(pAttachments)
                .pSubpasses(pSubPasses)
                .pDependencies(dependencies)
            val pRenderPass = stack.callocLong(1)
            val ret = vkCreateRenderPass(device.handle, renderPassInfo, null, pRenderPass)
            if (ret != VK_SUCCESS) {
                throw VulkanException("vkCreateRenderPass failed", ret)
            }
            handle = pRenderPass.get(0)
            subpasses = graphicsPipelineBuilders.mapIndexed { index, it -> SubpassInfo(it.build(), index) }
        }
    }

    fun buildGraphicsPipeline(block: VulkanGraphicsPipeline.Builder.() -> Unit): VulkanGraphicsPipeline {
        val builder = VulkanGraphicsPipeline.Builder(this)
        builder.block()
        return builder.build()
    }

    override fun close() {
        subpasses.forEach { it.pipeline.close() }
        vkDestroyRenderPass(device.handle, handle, null)
    }

}

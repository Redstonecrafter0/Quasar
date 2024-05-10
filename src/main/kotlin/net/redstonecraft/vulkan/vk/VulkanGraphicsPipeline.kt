package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.spvc.SPIRVCompiler
import net.redstonecraft.vulkan.spvc.ShaderType
import net.redstonecraft.vulkan.vk.enums.VulkanCulling
import net.redstonecraft.vulkan.vk.enums.VulkanPrimitive
import net.redstonecraft.vulkan.vk.interfaces.IHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*

class VulkanGraphicsPipeline private constructor(
    val renderPass: VulkanRenderPass,
    extent: VkExtent2D,
    vertexShader: VulkanVertexShaderModule,
    fragmentShader: VulkanFragmentShaderModule,
    primitive: VulkanPrimitive,
    culling: VulkanCulling
): IHandle<Long> {

    class Builder internal constructor(private val renderPass: VulkanRenderPass) {

        var extent: VkExtent2D? = null
        var vertexShader: VulkanVertexShaderModule? = null
        var fragmentShader: VulkanFragmentShaderModule? = null
        var primitive: VulkanPrimitive? = null
        var culling: VulkanCulling? = null

        internal fun build(): VulkanGraphicsPipeline {
            return VulkanGraphicsPipeline(renderPass, extent!!, vertexShader!!, fragmentShader!!, primitive!!, culling!!)
        }
    }

    val pipelineLayout = VulkanPipelineLayout(renderPass.device)
    override val handle: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val stages = VkPipelineShaderStageCreateInfo.calloc(2, stack)
                .put(vertexShader.getShaderStageCreateInfo(stack))
                .put(fragmentShader.getShaderStageCreateInfo(stack))
                .flip()
            val dynamicStates = stack.callocInt(2)
                .put(VK_DYNAMIC_STATE_VIEWPORT)
                .put(VK_DYNAMIC_STATE_SCISSOR)
                .flip()
            val dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack).`sType$Default`()
                .pDynamicStates(dynamicStates)
            val vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack).`sType$Default`() // TODO: add options when far enough in the tutorial
                .pVertexBindingDescriptions(null)
                .pVertexAttributeDescriptions(null)
            val inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack).`sType$Default`()
                .topology(primitive.primitive)
                .primitiveRestartEnable(false)
            val viewport = VkViewport.calloc(stack)
                .x(0F)
                .y(0F)
//                .width(swapChain.device.physicalDevice.surfaceCapabilities!!.extent.width().toFloat())
//                .height(swapChain.device.physicalDevice.surfaceCapabilities.extent.height().toFloat())
                .width(extent.width().toFloat())
                .height(extent.height().toFloat())
                .minDepth(0F)
                .maxDepth(1F)
            val scissorOffset = VkOffset2D.calloc(stack)
                .x(0)
                .y(0)
            val scissor = VkRect2D.calloc(stack)
                .offset(scissorOffset)
//                .extent(swapChain.renderPass.device.physicalDevice.surfaceCapabilities.extent)
                .extent(extent)
            val viewportState = VkPipelineViewportStateCreateInfo.calloc(stack).`sType$Default`()
                .viewportCount(1)
                .scissorCount(1)
            val rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack).`sType$Default`()
                .depthClampEnable(false)
                .rasterizerDiscardEnable(false)
                .polygonMode(VK_POLYGON_MODE_FILL)
                .lineWidth(1F)
                .cullMode(culling.cullMode)
                .frontFace(VK_FRONT_FACE_CLOCKWISE)
                .depthBiasEnable(false)
                .depthBiasConstantFactor(0F)
                .depthBiasClamp(0F)
                .depthBiasSlopeFactor(0F)
            val multiSampling = VkPipelineMultisampleStateCreateInfo.calloc(stack).`sType$Default`() // TODO: add options when far enough in the tutorial
                .sampleShadingEnable(false)
                .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
                .minSampleShading(1F)
                .pSampleMask(null)
                .alphaToCoverageEnable(false)
                .alphaToOneEnable(false)
            val colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(stack)
                .colorWriteMask(VK_COLOR_COMPONENT_R_BIT or VK_COLOR_COMPONENT_G_BIT or VK_COLOR_COMPONENT_B_BIT or VK_COLOR_COMPONENT_A_BIT)
                .blendEnable(true)
                .srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
                .dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
                .colorBlendOp(VK_BLEND_OP_ADD)
                .srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
                .dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO)
                .alphaBlendOp(VK_BLEND_OP_ADD)
            val colorBlendAttachments = VkPipelineColorBlendAttachmentState.calloc(1, stack).put(colorBlendAttachment).flip()
            val blendConstants = stack.callocFloat(4).put(0F).put(0F).put(0F).put(0F).flip()
            val colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack).`sType$Default`()
                .logicOpEnable(false)
                .logicOp(VK_LOGIC_OP_COPY)
                .attachmentCount(1)
                .pAttachments(colorBlendAttachments)
                .blendConstants(blendConstants)
            val pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(stack).`sType$Default`()
                .stageCount(stages.capacity())
                .pStages(stages)
                .pVertexInputState(vertexInputInfo)
                .pInputAssemblyState(inputAssembly)
                .pViewportState(viewportState)
                .pRasterizationState(rasterizer)
                .pMultisampleState(multiSampling)
                .pDepthStencilState(null)
                .pColorBlendState(colorBlending)
                .pDynamicState(dynamicState)
                .layout(pipelineLayout.handle)
                .renderPass(renderPass.handle)
                .subpass(0)
                .basePipelineHandle(VK_NULL_HANDLE)
                .basePipelineIndex(-1)
            val pipelineInfos = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                .put(pipelineInfo)
                .flip()
            val pGraphicsPipeline = stack.callocLong(1)
            val ret1 = vkCreateGraphicsPipelines(renderPass.device.handle, VK_NULL_HANDLE, pipelineInfos, null, pGraphicsPipeline)
            if (ret1 != VK_SUCCESS) {
                throw VulkanException("vkCreateGraphicsPipelines failed", ret1)
            }
            handle = pGraphicsPipeline.get(0)
        }
    }

    override fun close() {
        vkDestroyPipeline(renderPass.device.handle, handle, null)
        pipelineLayout.close()
    }

}

package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.util.Color
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkClearColorValue
import org.lwjgl.vulkan.VkClearValue
import org.lwjgl.vulkan.VkExtent2D
import org.lwjgl.vulkan.VkOffset2D
import org.lwjgl.vulkan.VkRect2D
import org.lwjgl.vulkan.VkRenderPassBeginInfo
import org.lwjgl.vulkan.VkViewport

class VulkanCommandBufferRecorder internal constructor(private val commandBuffer: VulkanCommandBuffer) {

    inner class RenderPassBuilder internal constructor(val renderPass: VulkanRenderPass) {

        private val graphicsPipelines = mutableListOf<GraphicsPipelineBuilder>()

        var framebuffer: VulkanFramebuffer? = null
        var offset: Pair<Int, Int> = 0 to 0
        var extent: VkExtent2D? = null
        var clearColor: Color = Color(0F, 0F, 0F, 1F)

        fun graphicsPipeline(graphicsPipeline: VulkanGraphicsPipeline, block: GraphicsPipelineBuilder.() -> Unit) {
            val builder = GraphicsPipelineBuilder(graphicsPipeline)
            builder.block()
            graphicsPipelines += builder
        }

        internal fun build() {
            requireNotNull(framebuffer) { "framebuffer must be not null" }
            requireNotNull(extent) { "extent must be not null" }
            MemoryStack.stackPush().use { stack ->
                val offset = VkOffset2D.calloc(stack)
                    .x(offset.first)
                    .y(offset.second)
                val renderArea = VkRect2D.calloc(stack)
                    .extent(extent!!)
                    .offset(offset)
                val clearColorBuffer = stack.callocFloat(4)
                    .put(clearColor.r)
                    .put(clearColor.g)
                    .put(clearColor.b)
                    .put(clearColor.a)
                    .flip()
                val clearColorValue = VkClearColorValue.calloc(stack)
                    .float32(clearColorBuffer)
                val clearValue = VkClearValue.calloc(stack)
                    .color(clearColorValue)
                val clearValueBuffer = VkClearValue.calloc(1, stack)
                    .put(clearValue)
                    .flip()
                val renderPassInfo = VkRenderPassBeginInfo.calloc(stack).`sType$Default`()
                    .renderPass(renderPass.handle)
                    .framebuffer(framebuffer!!.handle)
                    .renderArea(renderArea)
                    .pClearValues(clearValueBuffer)
                vkCmdBeginRenderPass(commandBuffer.handle, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE)
                graphicsPipelines.forEach { it.build() }
                vkCmdEndRenderPass(commandBuffer.handle)
            }
        }

        inner class GraphicsPipelineBuilder internal constructor(val graphicsPipeline: VulkanGraphicsPipeline) {

            var viewportPos = 0F to 0F
            var viewportSize: Pair<Float, Float>? = null
            var viewportDepth = 0F to 1F
            var scissorPos = 0 to 0
            var scissorExtent: VkExtent2D? = null
            var vertexCount: Int = 3
            var instanceCount: Int = 1
            var firstVertex: Int = 0
            var firstInstance: Int = 0

            fun build() {
                requireNotNull(viewportSize) { "viewportSize must be not null" }
                requireNotNull(scissorExtent) { "scissorExtent must be not null" }
                MemoryStack.stackPush().use { stack ->
                    vkCmdBindPipeline(commandBuffer.handle, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline.handle)
                    val viewport = VkViewport.calloc(stack)
                        .x(viewportPos.first)
                        .y(viewportPos.second)
                        .width(viewportSize!!.first)
                        .height(viewportSize!!.second)
                        .minDepth(viewportDepth.first)
                        .maxDepth(viewportDepth.second)
                    val viewports = VkViewport.calloc(1, stack)
                        .put(viewport)
                        .flip()
                    vkCmdSetViewport(commandBuffer.handle, 0, viewports)
                    val offset = VkOffset2D.calloc(stack)
                        .x(scissorPos.first)
                        .y(scissorPos.second)
                    val scissor = VkRect2D.calloc(stack)
                        .offset(offset)
                        .extent(scissorExtent!!)
                    val scissors = VkRect2D.calloc(1, stack)
                        .put(scissor)
                        .flip()
                    vkCmdSetScissor(commandBuffer.handle, 0, scissors)
                    vkCmdDraw(commandBuffer.handle, vertexCount, instanceCount, firstVertex, firstInstance)
                }
            }
        }

    }

    fun renderPass(renderPass: VulkanRenderPass, block: RenderPassBuilder.() -> Unit): VulkanCommandBufferRecorder {
        val builder = RenderPassBuilder(renderPass)
        builder.block()
        builder.build()
        return this
    }

    fun end() {
        val ret = vkEndCommandBuffer(commandBuffer.handle)
        if (ret != VK_SUCCESS) {
            throw VulkanException("vkEndCommandBuffer failed", ret)
        }
    }

}

package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.util.Color
import net.redstonecraft.vulkan.vk.data.BufferCopyLocations
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkBufferCopy
import org.lwjgl.vulkan.VkClearColorValue
import org.lwjgl.vulkan.VkClearValue
import org.lwjgl.vulkan.VkExtent2D
import org.lwjgl.vulkan.VkOffset2D
import org.lwjgl.vulkan.VkRect2D
import org.lwjgl.vulkan.VkRenderPassBeginInfo
import org.lwjgl.vulkan.VkViewport
import kotlin.math.min

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

            private val vertexBuffers = mutableListOf<VulkanVertexBuffer>()

            var viewportPos = 0F to 0F
            var viewportSize: Pair<Float, Float>? = null
            var viewportDepth = 0F to 1F
            var scissorPos = 0 to 0
            var scissorExtent: VkExtent2D? = null
            /**
             * vertex or index count depending on whether an index buffer is specified
             * */
            var count: Int? = null
            /**
             * first vertex or first index depending on whether an index buffer is specified
             * */
            var first = 0
            var instanceCount = 1
            var firstInstance = 0

            var indexBuffer: VulkanIndexBuffer? = null
            var indexType = VK_INDEX_TYPE_UINT16

            fun bindVertexBuffer(vertexBuffer: VulkanVertexBuffer) {
                vertexBuffers += vertexBuffer
            }

            fun build() {
                requireNotNull(viewportSize) { "viewportSize must be not null" }
                requireNotNull(scissorExtent) { "scissorExtent must be not null" }
                requireNotNull(count) { "vertexCount must be not null" }
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
                    val pVertexBuffers = stack.callocLong(vertexBuffers.size)
                    val pOffsets = stack.callocLong(vertexBuffers.size)
                    for (i in vertexBuffers) {
                        pVertexBuffers.put(i.handle)
                        pOffsets.put(0)
                    }
                    pVertexBuffers.flip()
                    pOffsets.flip()
                    vkCmdBindVertexBuffers(commandBuffer.handle, 0, pVertexBuffers, pOffsets)
                    when {
                        indexBuffer == null -> {
                            vkCmdDraw(commandBuffer.handle, count!!, instanceCount, first, firstInstance)
                        }
                        indexBuffer != null -> {
                            vkCmdBindIndexBuffer(commandBuffer.handle, indexBuffer!!.handle, 0, indexType)
                            vkCmdDrawIndexed(commandBuffer.handle, count!!, instanceCount, first, 0, firstInstance)
                        }
                        else -> throw VulkanException("invalid state")
                    }
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

    fun copyBuffer(
        srcBuffer: VulkanBuffer,
        dstBuffer: VulkanBuffer,
        copyRegions: List<BufferCopyLocations> = listOf(BufferCopyLocations(0, 0, min(srcBuffer.size, dstBuffer.size)))
    ) {
        MemoryStack.stackPush().use { stack ->
            val pCopyRegions = VkBufferCopy.calloc(copyRegions.size)
            for (i in copyRegions) {
                val copyRegion = VkBufferCopy.calloc(stack)
                    .srcOffset(i.srcOffset)
                    .dstOffset(i.dstOffset)
                    .size(i.size)
                pCopyRegions.put(copyRegion)
            }
            pCopyRegions.flip()
            vkCmdCopyBuffer(commandBuffer.handle, srcBuffer.handle, dstBuffer.handle, pCopyRegions)
        }
    }

    fun transferStagingBuffer(
        buffer: VulkanStagingBuffer<*>,
        copyRegions: List<BufferCopyLocations> = listOf(BufferCopyLocations(0, 0, buffer.size))
    ) {
        copyBuffer(buffer, buffer.backingBuffer, copyRegions)
    }

}

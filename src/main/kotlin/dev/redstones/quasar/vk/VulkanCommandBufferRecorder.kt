package dev.redstones.quasar.vk

import dev.redstones.quasar.util.Color
import dev.redstones.quasar.vk.data.BufferCopyLocations
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK13.*
import kotlin.math.min

class VulkanCommandBufferRecorder internal constructor(private val commandBuffer: VulkanCommandBuffer) {

    inner class RenderPassBuilder internal constructor(val framebuffer: VulkanFramebuffer) {

        private val graphicsPipelines = mutableListOf<GraphicsPipelineBuilder>()

        var offset: Pair<Int, Int> = 0 to 0
        var extent: VkExtent2D? = null
        var clearColor: Color = Color(0F, 0F, 0F, 1F)

        /**
         * MUST be in the same order as when creating the [VulkanRenderPass]
         * */
        fun graphicsPipeline(block: GraphicsPipelineBuilder.() -> Unit) {
            val builder = GraphicsPipelineBuilder(framebuffer.renderPass.subpasses[graphicsPipelines.size])
            builder.block()
            graphicsPipelines += builder
        }

        internal fun build() {
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
                    .renderPass(framebuffer.renderPass.handle)
                    .framebuffer(framebuffer.handle)
                    .renderArea(renderArea)
                    .pClearValues(clearValueBuffer)
                vkCmdBeginRenderPass(commandBuffer.handle, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE)
                graphicsPipelines.first().build()
                if (graphicsPipelines.size > 1) {
                    for (i in 1..graphicsPipelines.lastIndex) {
                        vkCmdNextSubpass(commandBuffer.handle, VK_SUBPASS_CONTENTS_INLINE)
                        graphicsPipelines[i].build()
                    }
                }
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
                requireNotNull(count) { "vertexCount must be not null" }
                MemoryStack.stackPush().use { stack ->
                    vkCmdBindPipeline(commandBuffer.handle, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline.handle)
                    if (viewportSize != null) {
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
                    }
                    if (scissorExtent != null) {
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
                    }
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

    fun renderPass(framebuffer: VulkanFramebuffer, block: RenderPassBuilder.() -> Unit) {
        val builder = RenderPassBuilder(framebuffer)
        builder.block()
        builder.build()
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

    class ImageCopyBuilder(private val stack: MemoryStack) {

        internal val regions = mutableListOf<VkImageCopy>()

        fun region(
            extent: VkExtent3D,
            srcOffset: VkOffset3D = VkOffset3D.calloc(stack).x(0).y(0).z(0),
            dstOffset: VkOffset3D = VkOffset3D.calloc(stack).x(0).y(0).z(0)
        ) {
            VkImageCopy.calloc(stack)
                .srcSubresource(
                    VkImageSubresourceLayers.calloc(stack)
                        .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        .baseArrayLayer(0)
                        .layerCount(1)
                        .mipLevel(0)
                )
                .dstSubresource(
                    VkImageSubresourceLayers.calloc(stack)
                        .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        .baseArrayLayer(0)
                        .layerCount(1)
                        .mipLevel(0)
                )
                .srcOffset(srcOffset)
                .dstOffset(dstOffset)
                .extent(extent)
        }
    }

    fun copyImage(srcImage: VulkanImage, dstImage: VulkanImage, block: ImageCopyBuilder.() -> Unit = { region(srcImage.extent) }) {
        MemoryStack.stackPush().use { stack ->
            val builder = ImageCopyBuilder(stack)
            builder.block()
            val pRegions = VkImageCopy.calloc(builder.regions.size, stack)
            for (i in builder.regions) {
                pRegions.put(i)
            }
            pRegions.flip()
            vkCmdCopyImage(commandBuffer.handle, srcImage.handle, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, dstImage.handle, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, pRegions)
        }
    }

    class ImageBlitBuilder(private val stack: MemoryStack) {

        internal val regions = mutableListOf<VkImageBlit>()

        fun region(
            srcSize: VkOffset3D,
            dstSize: VkOffset3D,
            dstOffset: VkOffset3D = VkOffset3D.calloc(stack).x(0).y(0).z(0),
            srcOffset: VkOffset3D = VkOffset3D.calloc(stack).x(0).y(0).z(0)
        ) {
            val pSrcOffsets = VkOffset3D.calloc(2, stack)
                .put(srcOffset)
                .put(srcSize)
                .flip()
            val pDstOffsets = VkOffset3D.calloc(2, stack)
                .put(dstOffset)
                .put(dstSize)
                .flip()
            VkImageBlit.calloc(stack)
                .srcSubresource(
                    VkImageSubresourceLayers.calloc(stack)
                        .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        .baseArrayLayer(0)
                        .layerCount(1)
                        .mipLevel(0)
                )
                .dstSubresource(
                    VkImageSubresourceLayers.calloc(stack)
                        .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        .baseArrayLayer(0)
                        .layerCount(1)
                        .mipLevel(0)
                )
                .srcOffsets(pSrcOffsets)
                .dstOffsets(pDstOffsets)
        }
    }

    fun blitImage(
        srcImage: VulkanImage,
        dstImage: VulkanImage,
        linear: Boolean = true,
        block: ImageBlitBuilder.() -> Unit = {
            region(
                VkOffset3D.calloc(MemoryStack.stackGet())
                    .x(srcImage.extent.width())
                    .y(srcImage.extent.height())
                    .z(1),
                VkOffset3D.calloc(MemoryStack.stackGet())
                    .x(dstImage.extent.width())
                    .y(dstImage.extent.height())
                    .z(1)
            )
        }
    ) {
        MemoryStack.stackPush().use { stack ->
            val builder = ImageBlitBuilder(stack)
            builder.block()
            val pRegions = VkImageBlit.calloc(builder.regions.size, stack)
            for (i in builder.regions) {
                pRegions.put(i)
            }
            pRegions.flip()
            vkCmdBlitImage(commandBuffer.handle, srcImage.handle, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, dstImage.handle, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, pRegions, if (linear) VK_FILTER_LINEAR else VK_FILTER_NEAREST)
        }
    }

    fun imageMemoryBarrier(image: VulkanImage, newLayout: Int) { // this is temporary. TODO: overhaul memory barriers in general
        MemoryStack.stackPush().use { stack ->
            val imageBarrier = VkImageMemoryBarrier2.calloc(stack).`sType$Default`()
                .srcStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                .srcAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT)
                .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT or VK_ACCESS_2_MEMORY_READ_BIT)
                .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .newLayout(newLayout)
                .subresourceRange(
                    VkImageSubresourceRange.calloc(stack)
                        .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        .baseArrayLayer(0)
                        .layerCount(VK_REMAINING_MIP_LEVELS)
                        .baseMipLevel(0)
                        .layerCount(VK_REMAINING_ARRAY_LAYERS)
                )
                .image(image.handle)
            val pImageBarriers = VkImageMemoryBarrier2.calloc(1, stack)
                .put(imageBarrier)
                .flip()
            val depInfo = VkDependencyInfo.calloc(stack).`sType$Default`()
                .pImageMemoryBarriers(pImageBarriers)
            vkCmdPipelineBarrier2(commandBuffer.handle, depInfo)
        }
    }

}

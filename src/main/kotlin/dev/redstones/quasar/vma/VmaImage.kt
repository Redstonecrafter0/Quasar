package dev.redstones.quasar.vma

import dev.redstones.quasar.vk.VulkanImage
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkExtent3D
import org.lwjgl.vulkan.VkImageCreateInfo

class VmaImage private constructor(
    val allocator: VMAAllocator,
    format: Int,
    extent: VkExtent3D,
    usage: Int
): VulkanImage(0, format, extent) {

    class Builder internal constructor(private val allocator: VMAAllocator) {

        var format = VK_FORMAT_R16G16B16A16_SFLOAT
        var extent: VkExtent3D? = null
        var transferSrc = false
        var transferDst = false
        var colorAttachment = false
        var depthStencilAttachment = false
        var inputAttachment = false
        var compute = false
        var sampled = true

        fun build(): VmaImage {
            requireNotNull(extent) { "extent must be not null" }
            return VmaImage(
                allocator,
                format,
                extent!!,
                (if (transferSrc) VK_IMAGE_USAGE_TRANSFER_SRC_BIT else 0) or
                        (if (transferDst) VK_IMAGE_USAGE_TRANSFER_DST_BIT else 0) or
                        (if (colorAttachment) VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT else 0) or
                        (if (depthStencilAttachment) VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT else 0) or
                        (if (inputAttachment) VK_IMAGE_USAGE_INPUT_ATTACHMENT_BIT else 0) or
                        (if (compute) VK_IMAGE_USAGE_STORAGE_BIT else 0) or
                        (if (sampled) VK_IMAGE_USAGE_SAMPLED_BIT else 0)
            )
        }
    }

    private val allocation: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val imageInfo = VkImageCreateInfo.calloc(stack).`sType$Default`()
                .imageType(VK_IMAGE_TYPE_2D)
                .format(format)
                .extent(extent)
                .mipLevels(1)
                .arrayLayers(1)
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .tiling(VK_IMAGE_TILING_OPTIMAL) // TODO: use linear for cpu reading
                .usage(usage)
            val allocationInfo = VmaAllocationCreateInfo.calloc(stack)
                .usage(VMA_MEMORY_USAGE_GPU_ONLY)
                .requiredFlags(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)
            val pImage = stack.callocLong(1)
            val pAllocation = stack.callocPointer(1)
            vmaCreateImage(allocator.handle, imageInfo, allocationInfo, pImage, pAllocation, null)
            handle = pImage.get(0)
            allocation = pAllocation.get(0)
        }
    }

    override fun close() {
        super.close()
        vmaDestroyImage(allocator.handle, handle, allocation)
    }

}

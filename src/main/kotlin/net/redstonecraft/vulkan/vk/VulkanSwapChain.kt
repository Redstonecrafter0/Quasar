package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.vk.interfaces.IHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR
import kotlin.math.min

class VulkanSwapChain private constructor(
    val device: VulkanLogicalDevice,
    forceRenderAllPixels: Boolean
): IHandle<Long> {

    class Builder internal constructor(private val device: VulkanLogicalDevice) {
        var forceRenderAllPixels: Boolean = false

        internal fun build(): VulkanSwapChain {
            return VulkanSwapChain(device, forceRenderAllPixels)
        }
    }

    override val handle: Long
    val images: List<VulkanImage>
    val imageViews: List<VulkanImageView>

    init {
        MemoryStack.stackPush().use { stack ->
            if (device.physicalDevice.surface == null) {
                throw VulkanException("Can't create swap chain without a surface")
            }
            if (device.physicalDevice.surfaceCapabilities == null) {
                throw VulkanException("Can't create swap chain without surface capabilities")
            }
            if (device.physicalDevice.surfaceFormat == null) {
                throw VulkanException("Can't create swap chain without a surface format")
            }
            if (device.physicalDevice.presentMode == null) {
                throw VulkanException("Can't create swap chain without a present mode")
            }
            if (device.physicalDevice.queueFamilyIndices.graphicsFamily == null) {
                throw VulkanException("Can't create swap chain without a graphics family")
            }
            if (device.physicalDevice.queueFamilyIndices.presentFamily == null) {
                throw VulkanException("Can't create swap chain without a present family")
            }
            var imageCount = device.physicalDevice.surfaceCapabilities.handle.minImageCount() + 1
            if (device.physicalDevice.surfaceCapabilities.handle.maxImageCount() > 0) {
                imageCount = min(imageCount, device.physicalDevice.surfaceCapabilities.handle.maxImageCount())
            }
            val createInfo = VkSwapchainCreateInfoKHR.calloc(stack).`sType$Default`()
                .surface(device.physicalDevice.surface.handle)
                .minImageCount(imageCount)
                .imageFormat(device.physicalDevice.surfaceFormat.format)
                .imageColorSpace(device.physicalDevice.surfaceFormat.colorSpace)
                .imageExtent(device.physicalDevice.surfaceCapabilities.extent)
                .imageArrayLayers(1)
                .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                .preTransform(device.physicalDevice.surfaceCapabilities.handle.currentTransform())
                .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                .presentMode(device.physicalDevice.presentMode)
                .clipped(!forceRenderAllPixels)
                .oldSwapchain(VK_NULL_HANDLE) // TODO: also check [acquireNextImage]

            if (device.physicalDevice.queueFamilyIndices.graphicsFamily != device.physicalDevice.queueFamilyIndices.presentFamily && device.physicalDevice.queueFamilyIndices.graphicsFamily != device.physicalDevice.queueFamilyIndices.computeFamily) {
                val queueFamilyIndices = stack.callocInt(3)
                queueFamilyIndices.put(device.physicalDevice.queueFamilyIndices.graphicsFamily!!)
                queueFamilyIndices.put(device.physicalDevice.queueFamilyIndices.presentFamily!!)
                queueFamilyIndices.put(device.physicalDevice.queueFamilyIndices.computeFamily!!)
                queueFamilyIndices.flip()
                createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT)
                    .queueFamilyIndexCount(3)
                    .pQueueFamilyIndices(queueFamilyIndices)
            } else {
                createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .queueFamilyIndexCount(0)
                    .pQueueFamilyIndices(null)
            }
            val pSwapChain = stack.callocLong(1)
            val ret = vkCreateSwapchainKHR(device.handle, createInfo, null, pSwapChain)
            if (ret != VK_SUCCESS) {
                throw VulkanException("vkCreateSwapchainKHR failed", ret)
            }
            handle = pSwapChain.get(0)

            val pImageCount = stack.callocInt(1)
            vkGetSwapchainImagesKHR(device.handle, handle, pImageCount, null)
            val pImages = stack.callocLong(pImageCount.get(0))
            vkGetSwapchainImagesKHR(device.handle, handle, pImageCount, pImages)

            images = (0 until pImages.capacity()).map { VulkanImage(pImages.get(it)) }
            imageViews = images.map {
                device.buildImageView {
                    image = it.handle
                    format = device.physicalDevice.surfaceFormat.format
                }
            }
        }
    }

    /**
     * @return null if the swap chain needs to be recreated
     * */
    fun acquireNextImage(semaphore: VulkanSemaphore? = null, fence: VulkanFence? = null): Int? {
        return MemoryStack.stackPush().use { stack ->
            val pImageIndex = stack.callocInt(1)
            val ret = vkAcquireNextImageKHR(device.handle, handle, Long.MAX_VALUE, semaphore?.handle ?: VK_NULL_HANDLE, fence?.handle ?: VK_NULL_HANDLE, pImageIndex)
            if (ret == VK_ERROR_OUT_OF_DATE_KHR) {
                null
            } else {
                pImageIndex.get(0)
            }
        }
    }

    override fun close() {
        imageViews.forEach { it.close() }
        vkDestroySwapchainKHR(device.handle, handle, null)
    }

}

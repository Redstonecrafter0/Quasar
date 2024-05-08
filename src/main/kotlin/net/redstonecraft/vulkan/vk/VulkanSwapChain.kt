package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.vk.interfaces.IHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR
import kotlin.math.min

class VulkanSwapChain internal constructor(
    val device: VulkanLogicalDevice,
    forceRenderAllPixels: Boolean
): IHandle<Long> {

    class Builder internal constructor(private val device: VulkanLogicalDevice) {
        var forceRenderAllPixels: Boolean = false

        fun build() = VulkanSwapChain(device, forceRenderAllPixels)
    }

    override val handle: Long
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
                .oldSwapchain(VK_NULL_HANDLE)

            if (device.physicalDevice.queueFamilyIndices.graphicsFamily != device.physicalDevice.queueFamilyIndices.presentFamily) {
                val queueFamilyIndices = stack.callocInt(2)
                queueFamilyIndices.put(device.physicalDevice.queueFamilyIndices.graphicsFamily!!, device.physicalDevice.queueFamilyIndices.presentFamily!!)
                queueFamilyIndices.flip()
                createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT)
                    .queueFamilyIndexCount(2)
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
            val images = stack.callocLong(pImageCount.get(0))
            vkGetSwapchainImagesKHR(device.handle, handle, pImageCount, images)

            imageViews = (0 until images.capacity()).map {
                device.buildImageView(images.get(it), device.physicalDevice.surfaceFormat.format).build()
            }
        }
    }

    override fun close() {
        imageViews.forEach { it.close() }
        vkDestroySwapchainKHR(device.handle, handle, null)
    }

}
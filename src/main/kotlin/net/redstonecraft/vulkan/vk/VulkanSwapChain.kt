package net.redstonecraft.vulkan.vk

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR
import java.io.Closeable
import java.nio.LongBuffer
import kotlin.math.min

class VulkanSwapChain(
    private val swapChainSupport: VulkanPhysicalDeviceSwapChainSupport,
    private val device: VulkanLogicalDevice,
    surface: VulkanSurface,
    indices: VulkanQueueFamilyIndices,
    forceRenderAllPixels: Boolean
): Closeable {

    val swapChain: Long
    val swapChainImages: LongBuffer
    val swapChainImageFormat by swapChainSupport::preferredFormat
    val swapChainExtent by swapChainSupport::extent

    init {
        MemoryStack.stackPush().use { stack ->
            var imageCount = swapChainSupport.capabilities.minImageCount() + 1
            if (swapChainSupport.capabilities.maxImageCount() > 0) {
                imageCount = min(imageCount, swapChainSupport.capabilities.maxImageCount())
            }
            val createInfo = VkSwapchainCreateInfoKHR.calloc(stack).`sType$Default`()
                .surface(surface.surface)
                .minImageCount(imageCount)
                .imageFormat(swapChainSupport.preferredFormat.format())
                .imageColorSpace(swapChainSupport.formats.colorSpace())
                .imageExtent(swapChainSupport.extent)
                .imageArrayLayers(1)
                .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                .preTransform(swapChainSupport.capabilities.currentTransform())
                .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                .presentMode(swapChainSupport.preferredPresentMode)
                .clipped(!forceRenderAllPixels)
                .oldSwapchain(VK_NULL_HANDLE)

            val queueFamilyIndices = stack.callocInt(2)
            queueFamilyIndices.put(indices.graphicsFamily, indices.presentFamily)
            queueFamilyIndices.flip()

            if (indices.graphicsFamily != indices.presentFamily) {
                createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT)
                    .queueFamilyIndexCount(2)
                    .pQueueFamilyIndices(queueFamilyIndices)
            } else {
                createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .queueFamilyIndexCount(0)
                    .pQueueFamilyIndices(null)
            }
            val pSwapChain = stack.callocLong(1)
            val ret = vkCreateSwapchainKHR(device.device, createInfo, null, pSwapChain)
            if (ret != VK_SUCCESS) {
                throw VulkanException("vkCreateSwapchainKHR failed", ret)
            }
            swapChain = pSwapChain.get(0)

            val pImageCount = stack.callocInt(1)
            vkGetSwapchainImagesKHR(device.device, swapChain, pImageCount, null)
            swapChainImages = memCallocLong(pImageCount.get(0))
            vkGetSwapchainImagesKHR(device.device, swapChain, pImageCount, swapChainImages)
        }
    }

    override fun close() {
        memFree(swapChainImages)
        vkDestroySwapchainKHR(device.device, swapChain, null)
    }

}

package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.spvc.SPIRVCompiler
import net.redstonecraft.vulkan.vk.enums.VulkanCulling
import net.redstonecraft.vulkan.vk.enums.VulkanPrimitive
import org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME
import java.io.Closeable

class VulkanContext(
    window: Long,
    val shaderCompiler: SPIRVCompiler,
    val shaderPath: String,
    appName: String,
    appVersion: Triple<Int, Int, Int>,
    engineName: String = "No Engine",
    engineVersion: Triple<Int, Int, Int> = Triple(1, 0, 0),
    vSync: Boolean = false,
    enableValidation: Boolean = true,
    forceRenderAllPixels: Boolean = true,
    extensions: List<String> = listOf(VK_KHR_SWAPCHAIN_EXTENSION_NAME)
): Closeable {

    val instance = VulkanInstance(appName, appVersion, engineName, engineVersion, vSync, enableValidation, extensions)
    val debugMessenger = if (enableValidation) VulkanDebugMessenger(instance) else null
    val surface = VulkanSurface(instance, window)
    val physicalDevices = VulkanPhysicalDevices(instance, surface, window)
    val physicalDevice = physicalDevices.pickDevice()
    val device = VulkanLogicalDevice(instance, physicalDevice)
    val swapChain = VulkanSwapChain(physicalDevice.swapChainSupport, device, surface, physicalDevice.queueFamilyIndices, forceRenderAllPixels)
    val swapChainImageViews = VulkanSwapChainImageViews(swapChain, device)

    val graphicsPipeline = VulkanGraphicsPipeline(device, swapChain, shaderCompiler, shaderPath, VulkanPrimitive.TRIANGLE, VulkanCulling.OFF)

    override fun close() {
        shaderCompiler.close()
        graphicsPipeline.close()
        swapChainImageViews.close()
        swapChain.close()
        device.close()
        surface.close()
        physicalDevice.close()
        physicalDevices.close()
        debugMessenger?.close()
        instance.close()
    }

}

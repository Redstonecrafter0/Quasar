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
    deviceExtensions: List<String> = listOf(VK_KHR_SWAPCHAIN_EXTENSION_NAME)
): Closeable {

    val instance = VulkanInstance.build {
        this.appName = appName
        this.appVersion = appVersion
        this.engineName = engineName
        this.engineVersion = engineVersion
        this.vSync = vSync
        this.debug = enableValidation
        this.extensions = emptyList()
    }
    val surface = instance.buildSurface {
        this.window = window
    }
    val physicalDevice = instance.getBestPhysicalDevice(surface, deviceExtensions.toSet())

    val device = physicalDevice.buildLogicalDevice()

    val graphicsPipeline = VulkanGraphicsPipeline(device, physicalDevice.surfaceCapabilities!!.extent, shaderCompiler, shaderPath, VulkanPrimitive.TRIANGLE, VulkanCulling.OFF)

    val swapChain = device.buildSwapChain {
        this.forceRenderAllPixels = forceRenderAllPixels
        renderPass = graphicsPipeline.renderPass
    }

    override fun close() {
        shaderCompiler.close()
        graphicsPipeline.close()
        swapChain.close()
        device.close()
        surface.close()
        physicalDevice.close()
        instance.close()
    }

}

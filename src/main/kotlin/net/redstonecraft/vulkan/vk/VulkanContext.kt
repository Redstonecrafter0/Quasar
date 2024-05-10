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

    val renderPass = device.buildRenderPass {
        format = device.physicalDevice.surfaceFormat!!.format
    }

    val vertexShader = device.buildVertexShaderModule {
        shaderCompiler = this@VulkanContext.shaderCompiler
        this.path = "${shaderPath.removeSuffix("/")}/vert.glsl"
    }

    val fragmentShader = device.buildFragmentShaderModule {
        shaderCompiler = this@VulkanContext.shaderCompiler
        this.path = "${shaderPath.removeSuffix("/")}/frag.glsl"
    }

    val graphicsPipeline = renderPass.buildGraphicsPipeline {
        extent = physicalDevice.surfaceCapabilities!!.extent
        vertexShader = this@VulkanContext.vertexShader
        fragmentShader = this@VulkanContext.fragmentShader
        primitive = VulkanPrimitive.TRIANGLE
        culling = VulkanCulling.OFF
    }

    val swapChain = device.buildSwapChain {
        this.forceRenderAllPixels = forceRenderAllPixels
        renderPass = graphicsPipeline.renderPass
    }

    val commandPool = device.buildCommandPool {  }

    val commandBuffer = commandPool.buildCommandBuffer {  }

    init {
        val imageIndex = 0
        commandBuffer.begin()
            .renderPass(renderPass) {
                framebuffer = swapChain.framebuffers[imageIndex]
                extent = physicalDevice.surfaceCapabilities!!.extent
                graphicsPipeline(graphicsPipeline) {
                    viewportSize = physicalDevice.surfaceCapabilities.extent.width().toFloat() to physicalDevice.surfaceCapabilities.extent.height().toFloat()
                    scissorExtent = physicalDevice.surfaceCapabilities.extent
                }
            }
            .end()
    }

    fun drawFrame() {
    }

    override fun close() {
        commandPool.close()
        vertexShader.close()
        fragmentShader.close()
        shaderCompiler.close()
        renderPass.close()
        graphicsPipeline.close()
        swapChain.close()
        device.close()
        surface.close()
        physicalDevice.close()
        instance.close()
    }

}

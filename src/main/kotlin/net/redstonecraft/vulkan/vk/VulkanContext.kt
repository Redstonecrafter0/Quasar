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
    private val forceRenderAllPixels: Boolean = true,
    private val maxFramesInFlight: Int = 2,
    deviceExtensions: List<String> = listOf(VK_KHR_SWAPCHAIN_EXTENSION_NAME)
): Closeable {

    class Frame(
        val commandBuffer: VulkanCommandBuffer,
        val imageAvailableSemaphore: VulkanSemaphore,
        val renderFinishedSemaphore: VulkanSemaphore,
        val inFlightFence: VulkanFence
    )

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

    var swapChain = device.buildSwapChain {
        this.forceRenderAllPixels = forceRenderAllPixels
        renderPass = graphicsPipeline.renderPass
    }

    val commandPool = device.buildCommandPool {  }

    val frames = buildList {
        repeat(maxFramesInFlight) {
            add(Frame(
                commandPool.buildCommandBuffer {  },
                device.buildSemaphore(),
                device.buildSemaphore(),
                device.buildFence {  }
            ))
        }
    }

    var frame = 0

    fun notifyResize() {
        physicalDevice.surfaceCapabilities!!.notifyResize()
    }

    fun recreateSwapChain() {
        device.waitIdle()
        swapChain.close()
        swapChain = device.buildSwapChain {
            this.forceRenderAllPixels = this@VulkanContext.forceRenderAllPixels
            renderPass = graphicsPipeline.renderPass
        }
    }

    fun drawFrame() {
        frame++
        if (frame >= maxFramesInFlight) {
            frame = 0
        }
        val frame = frames[frame]
        frame.inFlightFence.waitForFence()
        val imageIndex = swapChain.acquireNextImage(frame.imageAvailableSemaphore)
        if (imageIndex == null) {
            recreateSwapChain()
            return
        }
        frame.inFlightFence.reset()
        frame.commandBuffer.reset()
        frame.commandBuffer.record {
            renderPass(renderPass) {
                framebuffer = swapChain.framebuffers[imageIndex]
                extent = physicalDevice.surfaceCapabilities!!.extent
                graphicsPipeline(graphicsPipeline) {
                    viewportSize = physicalDevice.surfaceCapabilities.extent.width().toFloat() to physicalDevice.surfaceCapabilities.extent.height().toFloat()
                    scissorExtent = physicalDevice.surfaceCapabilities.extent
                }
            }
        }
        device.graphicsQueue.submit(listOf(frame.commandBuffer), listOf(frame.imageAvailableSemaphore), listOf(frame.renderFinishedSemaphore), frame.inFlightFence)
        if (device.presentQueue.present(swapChain, imageIndex, listOf(frame.renderFinishedSemaphore))) {
            recreateSwapChain()
        }
    }

    override fun close() {
        device.waitIdle()
        frames.forEach {
            it.renderFinishedSemaphore.close()
            it.imageAvailableSemaphore.close()
            it.inFlightFence.close()
        }
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

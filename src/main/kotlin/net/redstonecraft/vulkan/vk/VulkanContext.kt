package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.spvc.SPIRVCompiler
import net.redstonecraft.vulkan.vk.enums.InputRate
import net.redstonecraft.vulkan.vk.enums.VulkanCulling
import net.redstonecraft.vulkan.vk.enums.VulkanPrimitive
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK12.*
import java.io.Closeable

class VulkanContext(
    private val window: Long,
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
        val transferCommandBuffer: VulkanCommandBuffer,
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
        this.window = this@VulkanContext.window
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

        binding(0, 5 * 4, InputRate.VERTEX) {
            attribute(0, VK_FORMAT_R32G32_SFLOAT, 0)
            attribute(1, VK_FORMAT_R32G32B32_SFLOAT, 2 * 4)
        }
    }

    val transferCommandPool = device.buildCommandPool {
        flags = flags or VK_COMMAND_POOL_CREATE_TRANSIENT_BIT
    }

    val transferCommandBuffer = transferCommandPool.buildCommandBuffer {  }

    val vertexBuffer = device.buildStagedVertexBuffer {
        size = 4L * 5 * Float.SIZE_BYTES
    }.apply {
        upload(0, 4 * 5, floatArrayOf(
            -0.5f, -0.5f, 1.0f, 0.0f, 0.0f,
            0.5f, -0.5f, 0.0f, 1.0f, 0.0f,
            0.5f, 0.5f, 0.0f, 0.0f, 1.0f,
            -0.5f, 0.5f, 1.0f, 1.0f, 1.0f
        ))
    }

    val indexBuffer = device.buildStagedIndexBuffer {
        size = 6L * Short.SIZE_BYTES
    }.apply {
        upload(0, 6, shortArrayOf(0, 1, 2, 2, 3, 0))
        transferCommandBuffer.record {
            transferStagingBuffer(this@apply)
        }
        device.graphicsQueue.submit(listOf(transferCommandBuffer))
        device.waitIdle()
        close()
    }

    var swapChain = device.buildSwapChain {
        this.forceRenderAllPixels = this@VulkanContext.forceRenderAllPixels
        renderPass = graphicsPipeline.renderPass
    }

    val commandPool = device.buildCommandPool {  }

    val transferSemaphore = device.buildSemaphore()

    val frames = buildList {
        repeat(maxFramesInFlight) {
            add(Frame(
                commandPool.buildCommandBuffer {  },
                transferCommandPool.buildCommandBuffer {  },
                device.buildSemaphore(),
                device.buildSemaphore(),
                device.buildFence {  }
            ))
        }
    }

    var frame = 0

    fun notifyResize() {
        MemoryStack.stackPush().use { stack ->
            val pWidth = stack.callocInt(1)
            val pHeight = stack.callocInt(1)
            glfwGetFramebufferSize(window, pWidth, pHeight)
            while (pWidth.get() == 0 || pHeight.get() == 0) {
                pWidth.clear()
                pHeight.clear()
                glfwGetFramebufferSize(window, pWidth, pHeight)
                glfwWaitEvents()
            }
            device.waitIdle()
            physicalDevice.surfaceCapabilities!!.notifyResize()
        }
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
        frame.transferCommandBuffer.reset()
        frame.transferCommandBuffer.record {
            transferStagingBuffer(vertexBuffer)
        }
        frame.commandBuffer.reset()
        frame.commandBuffer.record {
            renderPass(renderPass) {
                framebuffer = swapChain.framebuffers[imageIndex]
                extent = physicalDevice.surfaceCapabilities!!.extent
                graphicsPipeline(graphicsPipeline) {
                    viewportSize = physicalDevice.surfaceCapabilities.extent.width().toFloat() to physicalDevice.surfaceCapabilities.extent.height().toFloat()
                    scissorExtent = physicalDevice.surfaceCapabilities.extent
                    count = 6
                    indexBuffer = this@VulkanContext.indexBuffer.backingBuffer
                    bindVertexBuffer(vertexBuffer.backingBuffer)
                }
            }
        }
        device.graphicsQueue.submit(listOf(frame.transferCommandBuffer), emptyList(), listOf(transferSemaphore))
        device.graphicsQueue.submit(listOf(frame.commandBuffer), listOf(frame.imageAvailableSemaphore, transferSemaphore), listOf(frame.renderFinishedSemaphore), frame.inFlightFence)
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
        transferSemaphore.close()
        indexBuffer.backingBuffer.close()
        vertexBuffer.close()
        vertexBuffer.backingBuffer.close()
        transferCommandPool.close()
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

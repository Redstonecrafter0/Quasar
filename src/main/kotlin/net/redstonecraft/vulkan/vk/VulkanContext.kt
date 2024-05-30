package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.spvc.SPIRVCompiler
import net.redstonecraft.vulkan.vk.enums.InputRate
import net.redstonecraft.vulkan.vk.enums.VulkanCulling
import net.redstonecraft.vulkan.vk.enums.VulkanPrimitive
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK13.*
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
        val swapChainSemaphore: VulkanSemaphore,
        val renderSemaphore: VulkanSemaphore,
        val renderFence: VulkanFence,
        val commandBuffer: VulkanCommandBuffer
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



    val vertexShader = device.buildVertexShaderModule {
        shaderCompiler = this@VulkanContext.shaderCompiler
        this.path = "${shaderPath.removeSuffix("/")}/vert.glsl"
    }

    val fragmentShader = device.buildFragmentShaderModule {
        shaderCompiler = this@VulkanContext.shaderCompiler
        this.path = "${shaderPath.removeSuffix("/")}/frag.glsl"
    }

    val commandPool = device.buildCommandPool {  }


    val renderPass = device.buildRenderPass {
        val present = presentColorAttachment(device.physicalDevice.surfaceFormat!!.format) {}

        val firstPass = graphicsSubpass {
            extent = physicalDevice.surfaceCapabilities!!.extent
            vertexShader = this@VulkanContext.vertexShader
            fragmentShader = this@VulkanContext.fragmentShader
            primitive = VulkanPrimitive.TRIANGLE
            culling = VulkanCulling.OFF
            wireframe = false

            colorAttachmentRef(present)

            binding(0, 5 * Float.SIZE_BYTES, InputRate.VERTEX) {
                attribute(0, VK_FORMAT_R32G32_SFLOAT, 0)
                attribute(1, VK_FORMAT_R32G32B32_SFLOAT, 2 * 4)
            }
        }
        graphicsSubpass {
            extent = physicalDevice.surfaceCapabilities!!.extent
            vertexShader = this@VulkanContext.vertexShader
            fragmentShader = this@VulkanContext.fragmentShader
            primitive = VulkanPrimitive.TRIANGLE
            culling = VulkanCulling.OFF
            wireframe = true
            dependsOn += firstPass

            colorAttachmentRef(present)

            binding(0, 5 * Float.SIZE_BYTES, InputRate.VERTEX) {
                attribute(0, VK_FORMAT_R32G32_SFLOAT, 0)
                attribute(1, VK_FORMAT_R32G32B32_SFLOAT, 2 * 4)
            }
        }
    }

    val transferCommandPool = device.buildCommandPool {
        flags = flags or VK_COMMAND_POOL_CREATE_TRANSIENT_BIT
    }

    val transferCommandBuffer = transferCommandPool.buildCommandBuffer {  }

    val vertexBuffer = device.buildVertexBuffer {
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
    }

    val framebuffers = swapChain.imageViews.map {
        device.buildFramebuffer {
            extent = device.physicalDevice.surfaceCapabilities!!.extent
            imageView = it
            this.renderPass = this@VulkanContext.renderPass
        }
    }

    val frames = buildList {
        repeat(maxFramesInFlight) {
            add(Frame(
                device.buildSemaphore(),
                device.buildSemaphore(),
                device.buildFence(),
                commandPool.buildCommandBuffer()
            ))
        }
    }

    var frameI = 0

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
        }
    }

    var firstFrame = true

    fun drawFrame() {
        frameI++
        if (frameI >= maxFramesInFlight) {
            frameI = 0
        }
        frameI %= maxFramesInFlight
        val frame = frames[frameI]
        val nextFrame = frames[(frameI + 1) % maxFramesInFlight]
        nextFrame.renderFence.waitForFence()
        nextFrame.renderFence.reset()
        val imageIndex = swapChain.acquireNextImage(nextFrame.swapChainSemaphore)
        if (imageIndex == null) {
            recreateSwapChain()
            return
        }
        nextFrame.commandBuffer.reset()
        nextFrame.commandBuffer.record(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT) {
            renderPass(framebuffers[imageIndex]) {
                extent = physicalDevice.surfaceCapabilities!!.extent
                graphicsPipeline {
//                    viewportSize = physicalDevice.surfaceCapabilities.extent.width().toFloat() to physicalDevice.surfaceCapabilities.extent.height().toFloat()
//                    scissorExtent = physicalDevice.surfaceCapabilities.extent
                    count = 3
                    indexBuffer = this@VulkanContext.indexBuffer.backingBuffer
                    bindVertexBuffer(vertexBuffer)
                }
                graphicsPipeline {
//                    viewportSize = physicalDevice.surfaceCapabilities.extent.width().toFloat() to physicalDevice.surfaceCapabilities.extent.height().toFloat()
//                    scissorExtent = physicalDevice.surfaceCapabilities.extent
                    count = 3
                    first = 3
                    indexBuffer = this@VulkanContext.indexBuffer.backingBuffer
                    bindVertexBuffer(vertexBuffer)
                }
            }
        }
        device.graphicsQueue.submit(listOf(nextFrame.commandBuffer), listOf(nextFrame.swapChainSemaphore), listOf(nextFrame.renderSemaphore), nextFrame.renderFence)
        if (!firstFrame && device.presentQueue.present(swapChain, imageIndex, listOf(frame.renderSemaphore))) {
            recreateSwapChain()
        } else {
            firstFrame = false
        }
    }

    override fun close() {
        device.waitIdle()
        frames.forEach {
            it.renderSemaphore.close()
            it.swapChainSemaphore.close()
            it.renderFence.close()
        }
        framebuffers.forEach { it.close() }
        indexBuffer.backingBuffer.close()
        vertexBuffer.close()
        transferCommandPool.close()
        commandPool.close()
        vertexShader.close()
        fragmentShader.close()
        shaderCompiler.close()
        renderPass.close()
        swapChain.close()
        device.close()
        surface.close()
        physicalDevice.close()
        instance.close()
    }

}

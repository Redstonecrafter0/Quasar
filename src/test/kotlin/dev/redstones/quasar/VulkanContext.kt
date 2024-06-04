package dev.redstones.quasar

import dev.redstones.quasar.shaderc.*
import dev.redstones.quasar.spvc.*
import dev.redstones.quasar.vk.*
import dev.redstones.quasar.vk.enums.*
import dev.redstones.quasar.vma.*
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
        val commandBuffer: VulkanCommandBuffer,
        var imageIndex: Int? = null
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

    val allocator = device.buildVmaAllocator()



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

            inferVertexLayout()
//            binding(0, 5 * Float.SIZE_BYTES, InputRate.VERTEX) {
//                attribute(0, VK_FORMAT_R32G32_SFLOAT, 0)
//                attribute(1, VK_FORMAT_R32G32B32_SFLOAT, 2 * Float.SIZE_BYTES)
//            }
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

            inferVertexLayout()
//            binding(0, 5 * Float.SIZE_BYTES, InputRate.VERTEX) {
//                attribute(0, VK_FORMAT_R32G32_SFLOAT, 0)
//                attribute(1, VK_FORMAT_R32G32B32_SFLOAT, 2 * Float.SIZE_BYTES)
//            }
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
        this.maxFramesInFlight = this@VulkanContext.maxFramesInFlight
    }

    val framebuffers = swapChain.imageViews.map {
        device.buildFramebuffer {
            extent = device.physicalDevice.surfaceCapabilities!!.extent
            imageView = it
            this.renderPass = this@VulkanContext.renderPass
        }
    }

    val frames = buildList {
        repeat(swapChain.imageCount) {
            add(Frame(
                device.buildSemaphore(),
                device.buildSemaphore(),
                device.buildFence { isSignalled = false },
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

    fun drawFrame() {
        frameI++
        frameI %= swapChain.imageCount
        val renderFrame = frames[frameI]
        val presentFrame = frames[(frameI + 1) % swapChain.imageCount]
        if (presentFrame.imageIndex != null) {
            presentFrame.renderFence.waitForFence()
            presentFrame.renderFence.reset()
        }
        renderFrame.imageIndex = swapChain.acquireNextImage(renderFrame.swapChainSemaphore)
        if (renderFrame.imageIndex == null) {
            recreateSwapChain()
            return
        }
        renderFrame.commandBuffer.reset()
        renderFrame.commandBuffer.record(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT) {
            renderPass(framebuffers[renderFrame.imageIndex!!]) {
                extent = physicalDevice.surfaceCapabilities!!.extent
                graphicsPipeline {
                    count = 3
                    indexBuffer = this@VulkanContext.indexBuffer.backingBuffer
                    bindVertexBuffer(vertexBuffer)
                }
                graphicsPipeline {
                    count = 3
                    first = 3
                    indexBuffer = this@VulkanContext.indexBuffer.backingBuffer
                    bindVertexBuffer(vertexBuffer)
                }
            }
        }
        device.graphicsQueue.submit(listOf(renderFrame.commandBuffer), listOf(renderFrame.swapChainSemaphore), listOf(renderFrame.renderSemaphore), renderFrame.renderFence)
        if (presentFrame.imageIndex != null && device.presentQueue.present(swapChain, presentFrame.imageIndex!!, listOf(presentFrame.renderSemaphore))) {
            recreateSwapChain()
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
        allocator.close()
        device.close()
        surface.close()
        physicalDevice.close()
        instance.close()
    }

}

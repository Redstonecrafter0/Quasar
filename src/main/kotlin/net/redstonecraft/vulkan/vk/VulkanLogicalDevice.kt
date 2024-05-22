package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.vk.interfaces.IHandle
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK12.*

class VulkanLogicalDevice internal constructor(val physicalDevice: VulkanPhysicalDevice): IHandle<VkDevice> {

    override val handle: VkDevice
    val queues: Map<Int, VulkanQueue>

    init {
        MemoryStack.stackPush().use { stack ->
            val queueFamilySet = setOfNotNull(
                physicalDevice.queueFamilyIndices.graphicsFamily,
                physicalDevice.queueFamilyIndices.presentFamily
            )
            val queueCreateInfos = buildQueueCreateInfo(queueFamilySet, stack)
            val pExtensionNames = buildExtensionNames(stack, physicalDevice.extensions)
            val deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack)
            val createInfo = VkDeviceCreateInfo.calloc(stack).`sType$Default`()
                .pQueueCreateInfos(queueCreateInfos)
                .pEnabledFeatures(deviceFeatures)
                .ppEnabledExtensionNames(pExtensionNames)
            val pDevice = stack.callocPointer(1)
            val ret = vkCreateDevice(physicalDevice.handle, createInfo, null, pDevice)
            if (ret != VK_SUCCESS) {
                throw VulkanException("vkCreateDevice failed", ret)
            }
            handle = VkDevice(pDevice.get(0), physicalDevice.handle, createInfo)
            queues = queueFamilySet.associateWith {
                val pQueue = stack.callocPointer(1)
                vkGetDeviceQueue(handle, it, 0, pQueue)
                VulkanQueue(VkQueue(pQueue.get(0), handle))
            }
        }
    }

    val graphicsQueue by lazy { queues[physicalDevice.queueFamilyIndices.graphicsFamily]!! }
    val presentQueue by lazy { queues[physicalDevice.queueFamilyIndices.presentFamily]!! }

    private fun buildQueueCreateInfo(queueFamilySet: Set<Int>, stack: MemoryStack): VkDeviceQueueCreateInfo.Buffer {
        val queueCreateInfos = VkDeviceQueueCreateInfo.calloc(queueFamilySet.size, stack)
        val queuePriority = stack.callocFloat(1).put(1F).flip()
        queueFamilySet.forEach {
            queueCreateInfos.put(
                VkDeviceQueueCreateInfo.calloc(stack).`sType$Default`()
                    .queueFamilyIndex(it)
                    .pQueuePriorities(queuePriority)
            )
        }
        queueCreateInfos.flip()
        return queueCreateInfos
    }

    private fun buildExtensionNames(stack: MemoryStack, extensions: List<String>): PointerBuffer {
        val pExtensionNames = stack.callocPointer(extensions.size)
        for (i in extensions) {
            val extensionName = stack.UTF8(i)
            pExtensionNames.put(memAddress(extensionName))
        }
        pExtensionNames.flip()
        return pExtensionNames
    }

    fun buildSwapChain(block: VulkanSwapChain.Builder.() -> Unit): VulkanSwapChain {
        val builder = VulkanSwapChain.Builder(this)
        builder.block()
        return builder.build()
    }

    fun buildRenderPass(block: VulkanRenderPass.Builder.() -> Unit): VulkanRenderPass {
        val builder = VulkanRenderPass.Builder(this)
        builder.block()
        return builder.build()
    }

    fun buildImageView(block: VulkanImageView.Builder.() -> Unit): VulkanImageView {
        val builder = VulkanImageView.Builder(this)
        builder.block()
        return builder.build()
    }

    fun buildFramebuffer(block: VulkanFramebuffer.Builder.() -> Unit): VulkanFramebuffer {
        val builder = VulkanFramebuffer.Builder(this)
        builder.block()
        return builder.build()
    }

    fun buildVertexShaderModule(block: VulkanVertexShaderModule.Builder.() -> Unit): VulkanVertexShaderModule {
        val builder = VulkanVertexShaderModule.Builder(this)
        builder.block()
        return builder.build()
    }

    fun buildFragmentShaderModule(block: VulkanFragmentShaderModule.Builder.() -> Unit): VulkanFragmentShaderModule {
        val builder = VulkanFragmentShaderModule.Builder(this)
        builder.block()
        return builder.build()
    }

    fun buildComputeShaderModule(block: VulkanComputeShaderModule.Builder.() -> Unit): VulkanComputeShaderModule {
        val builder = VulkanComputeShaderModule.Builder(this)
        builder.block()
        return builder.build()
    }

    fun buildCommandPool(block: VulkanCommandPool.Builder.() -> Unit): VulkanCommandPool {
        val builder = VulkanCommandPool.Builder(this)
        builder.block()
        return builder.build()
    }

    fun buildVertexBuffer(block: VulkanVertexBuffer.Builder.() -> Unit): VulkanVertexBuffer {
        val builder = VulkanVertexBuffer.Builder(this)
        builder.block()
        return builder.build()
    }

    fun buildStagedVertexBuffer(block: VulkanVertexBuffer.Builder.() -> Unit): VulkanStagingBuffer<VulkanVertexBuffer> {
        return VulkanStagingBuffer(this, buildVertexBuffer(block))
    }

    fun buildIndexBuffer(block: VulkanIndexBuffer.Builder.() -> Unit): VulkanIndexBuffer {
        val builder = VulkanIndexBuffer.Builder(this)
        builder.block()
        return builder.build()
    }

    fun buildStagedIndexBuffer(block: VulkanIndexBuffer.Builder.() -> Unit): VulkanStagingBuffer<VulkanIndexBuffer> {
        return VulkanStagingBuffer(this, buildIndexBuffer(block))
    }

    fun buildFence(block: VulkanFence.Builder.() -> Unit): VulkanFence {
        val builder = VulkanFence.Builder(this)
        builder.block()
        return builder.build()
    }

    fun buildSemaphore() = VulkanSemaphore(this)

    fun waitIdle() {
        vkDeviceWaitIdle(handle)
    }

    override fun close() {
        vkDestroyDevice(handle, null)
    }

}

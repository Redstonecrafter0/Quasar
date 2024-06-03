package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.interfaces.IHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkPresentInfoKHR
import org.lwjgl.vulkan.VkQueue
import org.lwjgl.vulkan.VkSubmitInfo

class VulkanQueue internal constructor(override val handle: VkQueue): IHandle<VkQueue> {

    fun submit(
        commandBuffers: List<VulkanCommandBuffer>,
        waitSemaphores: List<VulkanSemaphore> = emptyList(),
        signalSemaphores: List<VulkanSemaphore> = emptyList(),
        fence: VulkanFence? = null
    ) {
        MemoryStack.stackPush().use { stack ->
            val pWaitStages = stack.callocInt(1)
                .put(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .flip()
            val pCommandBuffers = stack.callocPointer(commandBuffers.size)
            for (i in commandBuffers) {
                pCommandBuffers.put(i.handle)
            }
            pCommandBuffers.flip()
            val submitInfo = VkSubmitInfo.calloc(stack).`sType$Default`()
                .pWaitDstStageMask(pWaitStages)
                .pCommandBuffers(pCommandBuffers)
            if (waitSemaphores.isNotEmpty()) {
                submitInfo.waitSemaphoreCount(waitSemaphores.size)
                val pSemaphores = stack.callocLong(waitSemaphores.size)
                waitSemaphores.forEach { pSemaphores.put(it.handle) }
                pSemaphores.flip()
                submitInfo.pWaitSemaphores(pSemaphores)
            }
            if (signalSemaphores.isNotEmpty()) {
                val pSemaphores = stack.callocLong(signalSemaphores.size)
                signalSemaphores.forEach { pSemaphores.put(it.handle) }
                pSemaphores.flip()
                submitInfo.pSignalSemaphores(pSemaphores)
            }
            val ret = vkQueueSubmit(handle, submitInfo, fence?.handle ?: VK_NULL_HANDLE)
            if (ret != VK_SUCCESS) {
                throw VulkanException("vkQueueSubmit failed", ret)
            }
        }
    }

    fun present(swapChain: VulkanSwapChain, imageIndex: Int, waitSemaphores: List<VulkanSemaphore>): Boolean {
        return MemoryStack.stackPush().use { stack ->
            val pWaitSemaphores = stack.callocLong(waitSemaphores.size)
            for (i in waitSemaphores) {
                pWaitSemaphores.put(i.handle)
            }
            pWaitSemaphores.flip()
            val swapChains = stack.callocLong(1)
                .put(swapChain.handle)
                .flip()
            val imageIndices = stack.callocInt(1)
                .put(imageIndex)
                .flip()
            val presentInfo = VkPresentInfoKHR.calloc(stack).`sType$Default`()
                .pWaitSemaphores(pWaitSemaphores)
                .swapchainCount(1)
                .pSwapchains(swapChains)
                .pImageIndices(imageIndices)
                .pResults(null)
            val ret = vkQueuePresentKHR(handle, presentInfo)
            val recreate = ret == VK_ERROR_OUT_OF_DATE_KHR || ret == VK_SUBOPTIMAL_KHR
            if (ret != VK_SUCCESS && !recreate) {
                throw VulkanException("vkQueuePresentKHR failed", ret)
            }
            recreate
        }
    }

    override fun close() {
        // closed by logical device
    }
}

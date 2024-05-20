package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.vk.interfaces.IHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkBufferCreateInfo
import org.lwjgl.vulkan.VkMemoryRequirements
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties

open class VulkanBuffer internal constructor(
    internal val device: VulkanLogicalDevice,
    val size: Long,
    type: Int
): IHandle<Long> {

    final override val handle: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val bufferInfo = VkBufferCreateInfo.calloc(stack).`sType$Default`()
                .size(size)
                .usage(type)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
            val pBuffer = stack.callocLong(1)
            val ret = vkCreateBuffer(device.handle, bufferInfo, null, pBuffer)
            if (ret != VK_SUCCESS) {
                throw VulkanException("vkCreateBuffer failed", ret)
            }
            handle = pBuffer.get(0)
            val memoryRequirements = VkMemoryRequirements.calloc(stack)
            vkGetBufferMemoryRequirements(device.handle, handle, memoryRequirements)
            val physicalDeviceMemoryProperties = VkPhysicalDeviceMemoryProperties.calloc(stack)
            vkGetPhysicalDeviceMemoryProperties(device.physicalDevice.handle, physicalDeviceMemoryProperties)
            val memoryTypes = physicalDeviceMemoryProperties.memoryTypes()
            val typeFilter = 1 // TODO
            val properties = 1 // TODO
            var found: Int? = null
            for (i in 0 until physicalDeviceMemoryProperties.memoryTypeCount()) {
                if (typeFilter and (1 shl i) != 0 && (memoryTypes.get(i).propertyFlags() and properties) == properties) {
                    found = i
                    break
                }
            }
            found ?: throw VulkanException("no suitable memory found for buffer")
        }
    }

    private val memory = VulkanDeviceMemory(this)

    init {
        vkBindBufferMemory(device.handle, handle, memory.handle, 0)
    }

    fun upload(offset: Int, size: Int, data: FloatArray) {
        MemoryStack.stackPush().use { stack ->
            val dataBuffer = memCallocFloat(size)
                .put(data)
                .flip()
            val pData = stack.callocPointer(1)
            vkMapMemory(device.handle, memory.handle, offset.toLong(), size.toLong(), 0, pData)
            memCopy(dataBuffer, pData.getFloatBuffer(size))
            vkUnmapMemory(device.handle, memory.handle)
            memFree(dataBuffer)
        }
    }

    override fun close() {
        memory.close()
        vkDestroyBuffer(device.handle, handle, null)
    }

}

class VulkanVertexBuffer private constructor(device: VulkanLogicalDevice, size: Long): VulkanBuffer(device, size, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT) {

    class Builder internal constructor(private val device: VulkanLogicalDevice) {

        var size: Long? = null

        fun build(): VulkanVertexBuffer {
            requireNotNull(size) { "size must be not null" }
            return VulkanVertexBuffer(device, size!!)
        }
    }

}

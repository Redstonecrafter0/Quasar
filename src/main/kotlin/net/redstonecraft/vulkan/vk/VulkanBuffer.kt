package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.vk.interfaces.IHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkBufferCreateInfo

open class VulkanBuffer internal constructor(
    internal val device: VulkanLogicalDevice,
    val size: Long,
    type: Int,
    memoryProperties: Int
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
        }
    }

    @Suppress("LeakingThis")
    private val memory = VulkanDeviceMemory(this, memoryProperties)

    init {
        vkBindBufferMemory(device.handle, handle, memory.handle, 0)
    }

    fun upload(offset: Int, size: Int, data: FloatArray) {
        MemoryStack.stackPush().use { stack ->
            val dataBuffer = memCallocFloat(size * Float.SIZE_BYTES)
                .put(data)
                .flip()
            val pData = stack.callocPointer(1)
            vkMapMemory(device.handle, memory.handle, offset.toLong(), size.toLong() * Float.SIZE_BYTES, 0, pData)
            memCopy(dataBuffer, pData.getFloatBuffer(size * Float.SIZE_BYTES))
            vkUnmapMemory(device.handle, memory.handle)
            memFree(dataBuffer)
        }
    }

    fun upload(offset: Int, size: Int, data: IntArray) {
        MemoryStack.stackPush().use { stack ->
            val dataBuffer = memCallocInt(size * Int.SIZE_BYTES)
                .put(data)
                .flip()
            val pData = stack.callocPointer(1)
            vkMapMemory(device.handle, memory.handle, offset.toLong(), size.toLong() * Int.SIZE_BYTES, 0, pData)
            memCopy(dataBuffer, pData.getIntBuffer(size * Int.SIZE_BYTES))
            vkUnmapMemory(device.handle, memory.handle)
            memFree(dataBuffer)
        }
    }

    fun upload(offset: Int, size: Int, data: ShortArray) {
        MemoryStack.stackPush().use { stack ->
            val dataBuffer = memCallocShort(size * Short.SIZE_BYTES)
                .put(data)
                .flip()
            val pData = stack.callocPointer(1)
            vkMapMemory(device.handle, memory.handle, offset.toLong(), size.toLong() * Short.SIZE_BYTES, 0, pData)
            memCopy(dataBuffer, pData.getShortBuffer(size * Short.SIZE_BYTES))
            vkUnmapMemory(device.handle, memory.handle)
            memFree(dataBuffer)
        }
    }

    override fun close() {
        memory.close()
        vkDestroyBuffer(device.handle, handle, null)
    }

}

class VulkanVertexBuffer private constructor(device: VulkanLogicalDevice, size: Long, type: Int, memoryProperties: Int): VulkanBuffer(device, size, type, memoryProperties) {

    class Builder internal constructor(private val device: VulkanLogicalDevice) {

        var size: Long? = null
        var localMemory = true

        fun build(): VulkanVertexBuffer {
            requireNotNull(size) { "size must be not null" }
            val memoryProperties = if (localMemory) {
                VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
            } else {
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
            }
            val type = if (localMemory) {
                VK_BUFFER_USAGE_VERTEX_BUFFER_BIT or VK_BUFFER_USAGE_TRANSFER_DST_BIT
            } else {
                VK_BUFFER_USAGE_VERTEX_BUFFER_BIT
            }
            return VulkanVertexBuffer(device, size!!, type, memoryProperties)
        }
    }

}

class VulkanIndexBuffer private constructor(device: VulkanLogicalDevice, size: Long, type: Int, memoryProperties: Int): VulkanBuffer(device, size, type, memoryProperties) {

    class Builder internal constructor(private val device: VulkanLogicalDevice) {

        var size: Long? = null
        var localMemory = true

        fun build(): VulkanIndexBuffer {
            requireNotNull(size) { "size must be not null" }
            val memoryProperties = if (localMemory) {
                VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
            } else {
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
            }
            val type = if (localMemory) {
                VK_BUFFER_USAGE_INDEX_BUFFER_BIT or VK_BUFFER_USAGE_TRANSFER_DST_BIT
            } else {
                VK_BUFFER_USAGE_INDEX_BUFFER_BIT
            }
            return VulkanIndexBuffer(device, size!!, type, memoryProperties)
        }
    }

}

class VulkanStagingBuffer<T: VulkanBuffer> internal constructor(device: VulkanLogicalDevice, val backingBuffer: T): VulkanBuffer(device, backingBuffer.size, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)

package net.redstonecraft.vulkan.vma

import net.redstonecraft.vulkan.vk.VulkanLogicalDevice
import net.redstonecraft.vulkan.vk.interfaces.IHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.util.vma.VmaAllocatorCreateInfo

class VMAAllocator internal constructor(
    val device: VulkanLogicalDevice
): IHandle<Long> {

    override val handle: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val allocatorInfo = VmaAllocatorCreateInfo.calloc(stack)
                .device(device.handle)
                .physicalDevice(device.physicalDevice.handle)
                .instance(device.physicalDevice.instance.handle)
                .flags(VMA_ALLOCATOR_CREATE_BUFFER_DEVICE_ADDRESS_BIT)
            val pAllocator = stack.callocPointer(1)
            vmaCreateAllocator(allocatorInfo, pAllocator)
            handle = pAllocator.get(0)
        }
    }

    override fun close() {
        vmaDestroyAllocator(handle)
    }

}

fun VulkanLogicalDevice.buildVmaAllocator() = VMAAllocator(this)

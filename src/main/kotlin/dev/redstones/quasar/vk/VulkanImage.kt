package dev.redstones.quasar.vk

import dev.redstones.quasar.interfaces.IHandle
import org.lwjgl.vulkan.VkExtent3D

open class VulkanImage internal constructor(
    handle: Long,
    val format: Int,
    val extent: VkExtent3D,
) : IHandle<Long> {

    final override var handle: Long = handle
        protected set

    override fun close() {
        // in case of an image allocated by the swap chain nothing has to be done
    }

}

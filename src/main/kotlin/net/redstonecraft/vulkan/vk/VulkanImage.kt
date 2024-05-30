package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.vk.interfaces.IHandle

class VulkanImage internal constructor(override val handle: Long): IHandle<Long> {

    override fun close() {
        TODO("Not yet implemented")
    }

}

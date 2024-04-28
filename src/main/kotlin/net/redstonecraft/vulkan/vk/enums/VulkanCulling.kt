package net.redstonecraft.vulkan.vk.enums

import org.lwjgl.vulkan.VK12.*

enum class VulkanCulling(val cullMode: Int) {
    OFF(VK_CULL_MODE_NONE), FRONT(VK_CULL_MODE_FRONT_BIT), BACK(VK_CULL_MODE_BACK_BIT), BOTH(VK_CULL_MODE_FRONT_AND_BACK)
}

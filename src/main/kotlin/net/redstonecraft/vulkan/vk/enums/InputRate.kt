package net.redstonecraft.vulkan.vk.enums

import org.lwjgl.vulkan.VK12.*

enum class InputRate(val inputRate: Int) {
    VERTEX(VK_VERTEX_INPUT_RATE_VERTEX),
    INSTANCE(VK_VERTEX_INPUT_RATE_INSTANCE)
}

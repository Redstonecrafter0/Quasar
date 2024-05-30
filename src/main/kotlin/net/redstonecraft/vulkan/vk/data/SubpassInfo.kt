package net.redstonecraft.vulkan.vk.data

import net.redstonecraft.vulkan.vk.VulkanGraphicsPipeline

data class SubpassInfo(
    val pipeline: VulkanGraphicsPipeline, // TODO: abstract to allow compute
    val subpassIndex: Int
)

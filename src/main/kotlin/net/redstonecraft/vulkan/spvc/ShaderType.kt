package net.redstonecraft.vulkan.spvc

import org.lwjgl.util.shaderc.Shaderc.*
import org.lwjgl.vulkan.VK13.*

enum class ShaderType(val shadercType: Int, val stage: Int) {
    VERTEX(shaderc_vertex_shader, VK_SHADER_STAGE_VERTEX_BIT),
    FRAGMENT(shaderc_fragment_shader, VK_SHADER_STAGE_FRAGMENT_BIT),
    COMPUTE(shaderc_compute_shader, VK_SHADER_STAGE_COMPUTE_BIT)
}

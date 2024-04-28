package net.redstonecraft.vulkan.spvc

import org.lwjgl.util.shaderc.Shaderc.*

enum class ShaderType(val shadercType: Int) {
    VERTEX(shaderc_vertex_shader),
    FRAGMENT(shaderc_fragment_shader),
    COMPUTE(shaderc_compute_shader)
}

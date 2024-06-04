package net.redstonecraft.vulkan.spvc

import net.redstonecraft.vulkan.interfaces.IHandle
import net.redstonecraft.vulkan.vk.VulkanGraphicsPipeline
import net.redstonecraft.vulkan.vk.enums.InputRate
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.util.spvc.Spv.*
import org.lwjgl.util.spvc.Spvc.*
import org.lwjgl.util.spvc.SpvcReflectedResource
import org.lwjgl.vulkan.VK13.*

class SpvcResources(val compiler: SpvcCompiler) : IHandle<Long> {

    override val handle: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val pResources = stack.callocPointer(1)
            val ret = spvc_compiler_create_shader_resources(compiler.handle, pResources)
            if (ret != SPVC_SUCCESS) {
                throw SpvcException("spvc_compiler_create_shader_resources failed", ret)
            }
            handle = pResources.get(0)
        }
    }

    fun getResources(type: Int): List<SpvcReflectedResource> {
        return MemoryStack.stackPush().use { stack ->
            val pResourceList = stack.callocPointer(1)
            val pResourceSize = stack.callocPointer(1)
            val ret = spvc_resources_get_resource_list_for_type(handle, type, pResourceList, pResourceSize)
            if (ret != SPVC_SUCCESS) {
                throw SpvcException("spvc_resources_get_resource_list_for_type failed", ret)
            }
            val arrayPointer = pResourceList.get(0)
            val resourceSize = pResourceSize.get(0).toInt()
            buildList {
                repeat(resourceSize) {
                    add(SpvcReflectedResource(memByteBuffer(arrayPointer + it * SpvcReflectedResource.SIZEOF, SpvcReflectedResource.SIZEOF)))
                }
            }
        }
    }

    override fun close() {
        // closed by context
    }

}

fun VulkanGraphicsPipeline.Builder.inferVertexLayout() {
    SpvcContext().use { context ->
        val compiler = context.buildCompiler(vertexShader?.bytecode ?: throw NullPointerException("vertexShader is null"))
        val resources = compiler.createResources().getResources(SPVC_RESOURCE_TYPE_STAGE_INPUT)
        val followsNamingConvention = resources.all { it.nameString().startsWith("v") || it.nameString().startsWith("i") }
                && resources.any { it.nameString().startsWith("i") }
        val vertexTypeSize = resources.firstOrNull()?.let {
            val typeHandle = spvc_compiler_get_type_handle(compiler.handle, it.base_type_id())
            val baseType = spvc_type_get_basetype(typeHandle)
            when (baseType) {
                SPVC_BASETYPE_INT32 -> Int.SIZE_BYTES
                SPVC_BASETYPE_FP32 -> Float.SIZE_BYTES
                else -> null
            }
        } ?: throw SpvcException("can't infer vertex type")

        if (followsNamingConvention) {
            val vertexInputs = resources.filter { it.nameString().startsWith("v") }
            val instanceInputs = resources.filter { it.nameString().startsWith("i") }
            buildBinding(0, InputRate.VERTEX, vertexInputs, compiler, vertexTypeSize)
            buildBinding(1, InputRate.INSTANCE, instanceInputs, compiler, vertexTypeSize)
        } else {
            buildBinding(0, InputRate.VERTEX, resources, compiler, vertexTypeSize)
        }
    }
}

private fun VulkanGraphicsPipeline.Builder.buildBinding(
    binding: Int,
    inputRate: InputRate,
    resources: List<SpvcReflectedResource>,
    compiler: SpvcCompiler,
    vertexTypeSize: Int
) {
    binding(binding, resources.sumOf {
        val typeHandle = spvc_compiler_get_type_handle(compiler.handle, it.base_type_id())
        spvc_type_get_vector_size(typeHandle)
    } * vertexTypeSize, inputRate) {
        var offset = 0
        for (i in resources) {
            attribute(
                spvc_compiler_get_decoration(compiler.handle, i.id(), SpvDecorationLocation),
                getVkFormat(compiler, i),
                offset
            )
            val typeHandle = spvc_compiler_get_type_handle(compiler.handle, i.base_type_id())
            offset += spvc_type_get_vector_size(typeHandle) * vertexTypeSize
        }
    }
}

private fun getVkFormat(compiler: SpvcCompiler, resource: SpvcReflectedResource): Int {
    val typeHandle = spvc_compiler_get_type_handle(compiler.handle, resource.base_type_id())
    val vectorSize = spvc_type_get_vector_size(typeHandle)
    val baseType = spvc_type_get_basetype(typeHandle)

    return when (baseType) {
        SPVC_BASETYPE_INT32 -> when (vectorSize) {
            1 -> VK_FORMAT_R32_SINT
            2 -> VK_FORMAT_R32G32_SINT
            3 -> VK_FORMAT_R32G32B32_SINT
            4 -> VK_FORMAT_R32G32B32A32_SINT
            else -> throw SpvcException("can't infer vertex type")
        }

        SPVC_BASETYPE_FP32 -> when (vectorSize) {
            1 -> VK_FORMAT_R32_SFLOAT
            2 -> VK_FORMAT_R32G32_SFLOAT
            3 -> VK_FORMAT_R32G32B32_SFLOAT
            4 -> VK_FORMAT_R32G32B32A32_SFLOAT
            else -> throw SpvcException("can't infer vertex type")
        }

        else -> throw SpvcException("can't infer vertex type")
    }
}

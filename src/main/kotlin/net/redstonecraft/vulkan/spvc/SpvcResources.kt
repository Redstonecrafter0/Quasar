package net.redstonecraft.vulkan.spvc

import net.redstonecraft.vulkan.interfaces.IHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memByteBuffer
import org.lwjgl.system.MemoryUtil.memUTF8
import org.lwjgl.util.spvc.Spvc.*
import org.lwjgl.util.spvc.SpvcReflectedResource

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

    fun printResources() { // TODO: refactor
        MemoryStack.stackPush().use { stack ->
            val pResourceList = stack.callocPointer(1)
            val pResourceSize = stack.callocPointer(1)
            val ret = spvc_resources_get_resource_list_for_type(handle, SPVC_RESOURCE_TYPE_STAGE_INPUT, pResourceList, pResourceSize)
            if (ret != SPVC_SUCCESS) {
                throw SpvcException("spvc_resources_get_resource_list_for_type failed", ret)
            }
            val arrayPointer = pResourceList.get(0)
            val resourceSize = pResourceSize.get(0).toInt()
            val resources = buildList {
                repeat(resourceSize) {
                    add(SpvcReflectedResource(memByteBuffer(arrayPointer + it * SpvcReflectedResource.SIZEOF, SpvcReflectedResource.SIZEOF)))
                }
            }
            resources.forEach {
                println(it.nameString())
                val typeHandle = spvc_compiler_get_type_handle(compiler.handle, it.base_type_id())
                val vectorSize = spvc_type_get_vector_size(typeHandle)
                val baseType = spvc_type_get_basetype(typeHandle)
                println(vectorSize)

                when (baseType) {
                    SPVC_BASETYPE_INT32 -> println("int")
                    SPVC_BASETYPE_FP32 -> println("float")
                    else -> println(baseType)
                }
            }
        }
    }

    override fun close() {
        // closed by context
    }

}

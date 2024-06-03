package net.redstonecraft.vulkan.spvc

import net.redstonecraft.vulkan.interfaces.IHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.util.spvc.Spvc.*
import java.nio.ByteBuffer

class SpvcContext: IHandle<Long> {

    override val handle: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val pContext = stack.callocPointer(1)
            val ret = spvc_context_create(pContext)
            if (ret != SPVC_SUCCESS) {
                throw SpvcException("spvc_context_create failed", ret)
            }
            handle = pContext.get(0)
            spvc_context_set_error_callback(handle, {_, pError ->
                System.err.println("SPVC [ERROR]: ${memUTF8(pError)}")
            }, 0)
        }
    }

    fun parse(spirv: ByteBuffer) {
        MemoryStack.stackPush().use { stack ->
            val spirvInt = spirv.asIntBuffer()
            val pIr = stack.callocPointer(1)
            val ret = spvc_context_parse_spirv(handle, spirvInt, spirvInt.remaining().toLong(), pIr)
            if (ret != SPVC_SUCCESS) {
                throw SpvcException("spvc_context_parse_spirv failed", ret)
            }
            val compiler = SpvcCompiler(this, pIr.get(0)) // pIr gets owned by the compiler now
            compiler.createResources().printResources()
        }
    }

    override fun close() {
        spvc_context_destroy(handle)
    }

}

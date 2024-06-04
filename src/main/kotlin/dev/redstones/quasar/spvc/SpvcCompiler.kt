package dev.redstones.quasar.spvc

import dev.redstones.quasar.interfaces.IHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.spvc.Spvc.*

class SpvcCompiler(context: SpvcContext, ir: Long): IHandle<Long> {

    override val handle: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val pCompiler = stack.callocPointer(1)
            val ret = spvc_context_create_compiler(context.handle, SPVC_BACKEND_NONE, ir, SPVC_CAPTURE_MODE_TAKE_OWNERSHIP, pCompiler)
            if (ret != SPVC_SUCCESS) {
                throw SpvcException("spvc_context_create_compiler failed", ret)
            }
            handle = pCompiler.get(0)
        }
    }

    fun createResources(): SpvcResources {
        return SpvcResources(this)
    }

    override fun close() {
        // closed by context
    }

}

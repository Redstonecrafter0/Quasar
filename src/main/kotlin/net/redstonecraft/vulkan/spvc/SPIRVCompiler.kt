package net.redstonecraft.vulkan.spvc

import java.io.Closeable
import java.nio.ByteBuffer

interface SPIRVCompiler: Closeable {

    fun compile(path: String, type: ShaderType): ByteBuffer
    fun compileAssembly(path: String, type: ShaderType): String

    fun free(bytecode: ByteBuffer)

}

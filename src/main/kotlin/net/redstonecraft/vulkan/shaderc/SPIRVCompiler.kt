package net.redstonecraft.vulkan.shaderc

import java.io.Closeable
import java.nio.ByteBuffer

interface SPIRVCompiler: Closeable {

    fun compile(path: String, type: ShaderType): ByteBuffer
    fun compileToText(path: String, type: ShaderType): String

    fun free(bytecode: ByteBuffer)

}

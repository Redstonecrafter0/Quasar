package net.redstonecraft.vulkan.shaderc

import net.redstonecraft.vulkan.vfs.VirtualFileSystem
import net.redstonecraft.vulkan.interfaces.IHandle
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.util.shaderc.Shaderc.*
import org.lwjgl.util.shaderc.ShadercIncludeResolveI
import org.lwjgl.util.shaderc.ShadercIncludeResult
import org.lwjgl.util.shaderc.ShadercIncludeResultReleaseI
import java.nio.ByteBuffer

class GLSLCompiler(val vfs: VirtualFileSystem): SPIRVCompiler, IHandle<Long> {

    override val handle = shaderc_compiler_initialize()
    val compileOptions = shaderc_compile_options_initialize()

    private val bytecodeRefs = mutableMapOf<ByteBuffer, Long>()

    init {
        shaderc_compile_options_set_include_callbacks(compileOptions, ShadercIncludeResolver(), ShadercIncludeResultReleaser(), 0)
        shaderc_compile_options_set_source_language(compileOptions, shaderc_source_language_glsl)
//        shaderc_compile_options_set_optimization_level(compileOptions, shaderc_optimization_level_performance) // removes variable names fetched by spirv cross. this prevents detecting the naming convention used for instancing
        shaderc_compile_options_set_auto_map_locations(compileOptions, true)
        // TODO: shaderc_compile_options_set_auto_bind_uniforms()
    }

    override fun compile(path: String, type: ShaderType): ByteBuffer {
        val path = if (path.startsWith("/")) path.substring(1, path.lastIndex) else path
        val fullSrc = preProcess(path, type.shadercType)
        val result = shaderc_compile_into_spv(handle, fullSrc, type.shadercType, "/$path", "main", compileOptions)
        val ret = shaderc_result_get_compilation_status(result)
        if (ret != shaderc_compilation_status_success) {
            throw ShadercException(shaderc_result_get_error_message(result), ret)
        }
        val bytes = shaderc_result_get_bytes(result)!!
        bytecodeRefs += bytes to result
        return bytes
    }

    override fun compileToText(path: String, type: ShaderType): String {
        val path = if (path.startsWith("/")) path.substring(1, path.lastIndex) else path
        val fullSrc = preProcess(path, type.shadercType)
        val result = shaderc_compile_into_spv_assembly(handle, fullSrc, type.shadercType, "/$path", "main", compileOptions)
        val ret = shaderc_result_get_compilation_status(result)
        if (ret != shaderc_compilation_status_success) {
            throw ShadercException(shaderc_result_get_error_message(result), ret)
        }
        val bytes = shaderc_result_get_bytes(result)!!
        val text = memUTF8(bytes)
        shaderc_result_release(result)
        return text
    }

    override fun free(bytecode: ByteBuffer) {
        val result = bytecodeRefs[bytecode]
        if (result != null) {
            shaderc_result_release(result)
            bytecodeRefs -= bytecode
        }
    }

    private fun preProcess(path: String, type: Int): String {
        val preResult = shaderc_compile_into_preprocessed_text(handle, vfs.readFile("", "/$path"), type, "/$path", "main", compileOptions)
        val preRet = shaderc_result_get_compilation_status(preResult)
        if (preRet != shaderc_compilation_status_success) {
            throw ShadercException(shaderc_result_get_error_message(preResult), preRet)
        }
        val pFullSrc = shaderc_result_get_bytes(preResult)!!
        val fullSrc = memUTF8(pFullSrc)
        shaderc_result_release(preResult)
        return fullSrc
    }

    override fun close() {
        shaderc_compile_options_release(compileOptions)
        shaderc_compiler_release(handle)
    }

    inner class ShadercIncludeResolver: ShadercIncludeResolveI {
        override fun invoke(
            user_data: Long,
            requested_source: Long,
            type: Int,
            requesting_source: Long,
            include_depth: Long
        ): Long {
            return try {
                val requestedFileName = (if (type == shaderc_include_type_relative) "" else "/") + memUTF8(requested_source)
                val sourceFileName = memUTF8(requesting_source)
                val requestedFilePath = "${vfs.parent(sourceFileName)}/$requestedFileName"
                if (include_depth > 100) {
                    throw ShadercException("$requestedFilePath: Include depth too high")
                }
                val pSourceName = memUTF8(requestedFilePath)
                val pContent = memUTF8(vfs.readFile(sourceFileName, requestedFilePath))
                return ShadercIncludeResult.calloc()
                    .source_name(pSourceName)
                    .content(pContent)
                    .user_data(user_data)
                    .address()
            } catch (e: Throwable) {
                e.printStackTrace()
                val pSourceName = memUTF8("")
                val pContent = memUTF8("")
                ShadercIncludeResult.calloc()
                    .source_name(pSourceName)
                    .content(pContent)
                    .user_data(user_data)
                    .address()
            }
        }
    }

    inner class ShadercIncludeResultReleaser: ShadercIncludeResultReleaseI {
        override fun invoke(user_data: Long, include_result: Long) {
            val result = ShadercIncludeResult.create(include_result)
            memFree(result.source_name())
            memFree(result.content())
            result.free()
        }
    }

}

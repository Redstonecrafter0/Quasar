package dev.redstones.quasar.vk

import dev.redstones.quasar.shaderc.SPIRVCompiler
import dev.redstones.quasar.shaderc.ShaderType
import dev.redstones.quasar.interfaces.IHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo
import org.lwjgl.vulkan.VkShaderModuleCreateInfo
import java.nio.ByteBuffer

open class VulkanShaderModule internal constructor(val device: VulkanLogicalDevice, val shaderCompiler: SPIRVCompiler, path: String, private val type: ShaderType): IHandle<Long> {

    final override val handle: Long
    val bytecode: ByteBuffer

    init {
        MemoryStack.stackPush().use { stack ->
            bytecode = shaderCompiler.compile(path, type)
            val createInfo = VkShaderModuleCreateInfo.calloc(stack).`sType$Default`()
                .pCode(bytecode)
            val pShaderModule = stack.callocLong(1)
            val ret = vkCreateShaderModule(device.handle, createInfo, null, pShaderModule)
            if (ret != VK_SUCCESS) {
                throw VulkanException("vkCreateShaderModule failed", ret)
            }
            handle = pShaderModule.get(0)
        }
    }

    fun getShaderStageCreateInfo(stack: MemoryStack): VkPipelineShaderStageCreateInfo {
        val pName = stack.UTF8("main")
        return VkPipelineShaderStageCreateInfo.calloc(stack).`sType$Default`()
            .stage(type.stage)
            .module(handle)
            .pName(pName)
    }

    override fun close() {
        shaderCompiler.free(bytecode)
        vkDestroyShaderModule(device.handle, handle, null)
    }

}

class VulkanVertexShaderModule private constructor(device: VulkanLogicalDevice, shaderCompiler: SPIRVCompiler, path: String): VulkanShaderModule(device, shaderCompiler, path, ShaderType.VERTEX) {

    class Builder internal constructor(private val device: VulkanLogicalDevice) {
        var shaderCompiler: SPIRVCompiler? = null
        var path: String? = null

        internal fun build(): VulkanVertexShaderModule {
            requireNotNull(shaderCompiler) { "shaderCompiler must be not null" }
            requireNotNull(path) { "path must be not null" }
            return VulkanVertexShaderModule(device, shaderCompiler!!, path!!)
        }
    }

}

class VulkanFragmentShaderModule private constructor(device: VulkanLogicalDevice, shaderCompiler: SPIRVCompiler, path: String): VulkanShaderModule(device, shaderCompiler, path, ShaderType.FRAGMENT) {

    class Builder internal constructor(private val device: VulkanLogicalDevice) {
        var shaderCompiler: SPIRVCompiler? = null
        var path: String? = null

        internal fun build(): VulkanFragmentShaderModule {
            requireNotNull(shaderCompiler) { "shaderCompiler must be not null" }
            requireNotNull(path) { "path must be not null" }
            return VulkanFragmentShaderModule(device, shaderCompiler!!, path!!)
        }
    }

}

class VulkanComputeShaderModule private constructor(device: VulkanLogicalDevice, shaderCompiler: SPIRVCompiler, path: String): VulkanShaderModule(device, shaderCompiler, path, ShaderType.COMPUTE) {

    class Builder internal constructor(private val device: VulkanLogicalDevice) {
        var shaderCompiler: SPIRVCompiler? = null
        var path: String? = null

        internal fun build(): VulkanComputeShaderModule {
            requireNotNull(shaderCompiler) { "shaderCompiler must be not null" }
            requireNotNull(path) { "path must be not null" }
            return VulkanComputeShaderModule(device, shaderCompiler!!, path!!)
        }
    }

}

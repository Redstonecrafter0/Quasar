package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.spvc.SPIRVCompiler
import net.redstonecraft.vulkan.spvc.ShaderType
import net.redstonecraft.vulkan.vk.interfaces.IHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo
import org.lwjgl.vulkan.VkShaderModuleCreateInfo

open class VulkanShaderModule(val device: VulkanLogicalDevice, shaderCompiler: SPIRVCompiler, path: String, private val type: ShaderType): IHandle<Long> {

    final override val handle: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val bytecode = shaderCompiler.compile(path, type)
            val createInfo = VkShaderModuleCreateInfo.calloc(stack).`sType$Default`()
                .pCode(bytecode)
            val pShaderModule = stack.callocLong(1)
            val ret = vkCreateShaderModule(device.handle, createInfo, null, pShaderModule)
            if (ret != VK_SUCCESS) {
                throw VulkanException("vkCreateShaderModule failed", ret)
            }
            handle = pShaderModule.get(0)
            shaderCompiler.free(bytecode)
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
        vkDestroyShaderModule(device.handle, handle, null)
    }

}

class VulkanVertexShaderModule(device: VulkanLogicalDevice, shaderCompiler: SPIRVCompiler, path: String): VulkanShaderModule(device, shaderCompiler, path, ShaderType.VERTEX)

class VulkanFragmentShaderModule(device: VulkanLogicalDevice, shaderCompiler: SPIRVCompiler, path: String): VulkanShaderModule(device, shaderCompiler, path, ShaderType.FRAGMENT)

class VulkanComputeShaderModule(device: VulkanLogicalDevice, shaderCompiler: SPIRVCompiler, path: String): VulkanShaderModule(device, shaderCompiler, path, ShaderType.COMPUTE)

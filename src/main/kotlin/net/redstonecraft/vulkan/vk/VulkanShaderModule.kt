package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.spvc.SPIRVCompiler
import net.redstonecraft.vulkan.spvc.ShaderType
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkShaderModuleCreateInfo
import java.io.Closeable

class VulkanShaderModule(val device: VulkanLogicalDevice, shaderCompiler: SPIRVCompiler, path: String, type: ShaderType): Closeable {

    val shaderModule: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val bytecode = shaderCompiler.compile(path, type)
            val createInfo = VkShaderModuleCreateInfo.calloc(stack).`sType$Default`()
                .pCode(bytecode)
            val pShaderModule = stack.callocLong(1)
            val ret = vkCreateShaderModule(device.device, createInfo, null, pShaderModule)
            if (ret != VK_SUCCESS) {
                throw VulkanException("vkCreateShaderModule failed", ret)
            }
            shaderModule = pShaderModule.get(0)
            shaderCompiler.free(bytecode)
        }
    }

    override fun close() {
        vkDestroyShaderModule(device.device, shaderModule, null)
    }

}

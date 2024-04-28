package net.redstonecraft.vulkan.vk

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.EXTDebugUtils.*
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT
import java.io.Closeable

class VulkanDebugMessenger(private val instance: VulkanInstance): Closeable {

    companion object {

        private val severityLevels = mapOf(
            VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT to "VERBOSE",
            VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT to "INFO",
            VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT to "WARNING",
            VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT to "ERROR"
        )

        private val messageTypeMap = mapOf(
            VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT to "PERFORMANCE",
            VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT to "VALIDATION",
            VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT to "GENERAL"
        )

    }

    val debugMessenger: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val createInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack).`sType$Default`()
                .messageSeverity(
                    VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT
                            or VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT
                            or VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT
                            or VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
                ).messageType(
                    VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
                            or VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT
                            or VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT
                ).pfnUserCallback { messageSeverity, messageTypes, pCallbackData, pUserData ->
                    val callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData)
                    System.err.println("Vulkan validation layer [${severityLevels[messageSeverity]}] [${messageTypeMap[messageTypes]}]: ${callbackData.pMessageString()}")
                    VK_FALSE
                }
            val pDebugMessenger = stack.callocLong(1)
            val ret = vkCreateDebugUtilsMessengerEXT(instance.instance, createInfo, null, pDebugMessenger)
            if (ret != VK_SUCCESS) {
                throw VulkanException("vkCreateDebugUtilsMessengerEXT failed", ret)
            }
            debugMessenger = pDebugMessenger.get(0)
        }
    }

    override fun close() {
        vkDestroyDebugUtilsMessengerEXT(instance.instance, debugMessenger, null)
    }

}

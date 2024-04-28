package net.redstonecraft.vulkan.vk

import org.lwjgl.glfw.GLFWVulkan.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.EXTDebugUtils.*
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkApplicationInfo
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkInstanceCreateInfo
import org.lwjgl.vulkan.VkLayerProperties
import java.io.Closeable

class VulkanInstance(
    appName: String,
    appVersion: Triple<Int, Int, Int>,
    engineName: String = "No Engine",
    engineVersion: Triple<Int, Int, Int>,
    val vSync: Boolean,
    val enableValidation: Boolean,
    val extensions: List<String>
): Closeable {

    companion object {
        const val validationLayer = "VK_LAYER_KHRONOS_validation"
    }

    val instance: VkInstance

    init {
        MemoryStack.stackPush().use { stack ->
            val pAppName = stack.UTF8(appName)
            val pEngineName = stack.UTF8(engineName)
            val appInfo = VkApplicationInfo.calloc(stack).`sType$Default`()
                .pApplicationName(pAppName)
                .applicationVersion(VK_MAKE_VERSION(appVersion.first, appVersion.second, appVersion.third))
                .pEngineName(pEngineName)
                .engineVersion(VK_MAKE_VERSION(engineVersion.first, engineVersion.second, engineVersion.third))
                .apiVersion(VK_API_VERSION_1_2)
            val glfwExtensions = glfwGetRequiredInstanceExtensions()!!
            val extensions = stack.callocPointer(glfwExtensions.capacity() + if (enableValidation) 1 else 0)
            for (i in 0 until glfwExtensions.capacity()) {
                extensions.put(glfwExtensions.get(i))
            }
            if (enableValidation) {
                val extension = stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME)
                extensions.put(extension)
            }
            extensions.flip()
            val createInfo = VkInstanceCreateInfo.calloc(stack).`sType$Default`()
                .pApplicationInfo(appInfo)
                .ppEnabledExtensionNames(extensions)
            if (enableValidation) {
                if (!checkValidationLayerSupport(stack)) {
                    throw VulkanException("Asked for $validationLayer but it is not present")
                }
                val layerNames = stack.callocPointer(1)
                val layerName = stack.UTF8(validationLayer)
                layerNames.put(layerName)
                layerNames.flip()
                createInfo.ppEnabledLayerNames(layerNames)
            }
            val pInstance = stack.callocPointer(1)
            val ret = vkCreateInstance(createInfo, null, pInstance)
            if (ret != VK_SUCCESS) {
                throw VulkanException("vkCreateInstance failed", ret)
            }
            instance = VkInstance(pInstance.get(0), createInfo)
        }
    }
    
    private fun checkValidationLayerSupport(stack: MemoryStack): Boolean {
        val layerCount = stack.callocInt(1)
        vkEnumerateInstanceLayerProperties(layerCount, null)
        val availableLayers = VkLayerProperties.calloc(layerCount.get(0), stack)
        vkEnumerateInstanceLayerProperties(layerCount, availableLayers)

        for (i in 0 until availableLayers.capacity()) {
            if (availableLayers.get(i).layerNameString() == validationLayer) {
                return true
            }
        }
        return false
    }

    override fun close() {
        vkDestroyInstance(instance, null)
    }

}

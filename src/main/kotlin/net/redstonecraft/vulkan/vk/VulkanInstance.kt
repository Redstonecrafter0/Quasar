package net.redstonecraft.vulkan.vk

import net.redstonecraft.vulkan.vk.interfaces.IHandle
import org.lwjgl.glfw.GLFWVulkan.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.EXTDebugUtils.*
import org.lwjgl.vulkan.EXTSwapchainColorspace.*
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkApplicationInfo
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkInstanceCreateInfo
import org.lwjgl.vulkan.VkLayerProperties

class VulkanInstance private constructor(
    appName: String,
    appVersion: Triple<Int, Int, Int>,
    engineName: String = "No Engine",
    engineVersion: Triple<Int, Int, Int>,
    val vSync: Boolean,
    val hdr: Boolean,
    val debug: Boolean,
    private val extensions: MutableSet<String>
): IHandle<VkInstance> {

    companion object {

        private const val validationLayer = "VK_LAYER_KHRONOS_validation"

        fun build(block: Builder.() -> Unit): VulkanInstance {
            val builder = Builder()
            builder.block()
            return builder.build()
        }
    }

    class Builder internal constructor() {
        var appName = "Vulkan"
        var appVersion = Triple(0, 0, 0)
        var engineName = "No Engine"
        var engineVersion = Triple(0, 0, 0)
        var vSync = false
        var hdr = false
        var debug = true
        var extensions = emptyList<String>()

        internal fun build() = VulkanInstance(appName, appVersion, engineName, engineVersion, vSync, hdr, debug, extensions.toMutableSet())
    }

    override val handle: VkInstance
    private val debugMessenger: VulkanDebugMessenger?

    init {
        MemoryStack.stackPush().use { stack ->
            val pAppName = stack.UTF8(appName)
            val pEngineName = stack.UTF8(engineName)
            val appInfo = VkApplicationInfo.calloc(stack).`sType$Default`()
                .pApplicationName(pAppName)
                .applicationVersion(VK_MAKE_API_VERSION(0, appVersion.first, appVersion.second, appVersion.third))
                .pEngineName(pEngineName)
                .engineVersion(VK_MAKE_API_VERSION(0, engineVersion.first, engineVersion.second, engineVersion.third))
                .apiVersion(VK_API_VERSION_1_3)
            if (debug) {
                extensions += VK_EXT_DEBUG_UTILS_EXTENSION_NAME
            }
            if (hdr) {
                extensions += VK_EXT_SWAPCHAIN_COLOR_SPACE_EXTENSION_NAME
            }
            val glfwExtensions = glfwGetRequiredInstanceExtensions()!!
            val pExtensions = stack.callocPointer(glfwExtensions.capacity() + extensions.size)
            for (i in 0 until glfwExtensions.capacity()) {
                pExtensions.put(glfwExtensions.get(i))
            }
            for (i in extensions) {
                pExtensions.put(stack.UTF8(i))
            }
            pExtensions.flip()
            val createInfo = VkInstanceCreateInfo.calloc(stack).`sType$Default`()
                .pApplicationInfo(appInfo)
                .ppEnabledExtensionNames(pExtensions)
            if (debug) {
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
            handle = VkInstance(pInstance.get(0), createInfo)
            debugMessenger = if (debug) VulkanDebugMessenger(this) else null
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

    fun buildSurface(block: VulkanSurface.Builder.() -> Unit): VulkanSurface {
        val builder = VulkanSurface.Builder(this)
        builder.block()
        return builder.build()
    }

    fun selectPhysicalDevice(surface: VulkanSurface?, deviceExtensions: Set<String> = emptySet(), block: List<VulkanPhysicalDevice>.() -> VulkanPhysicalDevice): VulkanPhysicalDevice {
        return MemoryStack.stackPush().use { stack ->
            val deviceCount = stack.callocInt(1)
            vkEnumeratePhysicalDevices(handle, deviceCount, null)
            val pDevices = stack.callocPointer(deviceCount.get(0))
            vkEnumeratePhysicalDevices(handle, deviceCount, pDevices)

            val devices = (0 until deviceCount.get(0)).map {
                VulkanPhysicalDevice(this, surface, pDevices.get(it), deviceExtensions.toList())
            }.filter {
                val valid = it.isValid
                if (!valid) it.close()
                valid
            }
            val selectedDevice = devices.block()
            (devices - selectedDevice).forEach { it.close() }
            selectedDevice
        }
    }

    fun getBestPhysicalDevice(surface: VulkanSurface?, deviceExtensions: Set<String> = emptySet()) = selectPhysicalDevice(surface, deviceExtensions) { pickBestDevice() }

    override fun close() {
        debugMessenger?.close()
        vkDestroyInstance(handle, null)
    }

}

fun List<VulkanPhysicalDevice>.pickBestDevice(): VulkanPhysicalDevice {
    return groupBy { it.isDiscrete }
        .mapValues { it.value.sortedByDescending { i -> i.memory } }
        .asSequence()
        .sortedBy { if (it.key) 0 else 1 }
        .map { it.value }
        .flatten()
        .first()
}

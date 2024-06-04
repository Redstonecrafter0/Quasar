package dev.redstones.quasar.vk

class VulkanException(message: String, ret: Int? = null): Exception(transformMessage(message, ret)) {

    companion object {

        fun transformMessage(message: String, ret: Int?): String {
            return if (ret != null) {
                "${map[ret] ?: ret.toString()}: $message"
            } else {
                message
            }
        }

        val map = mapOf(
            0 to "VK_SUCCESS",
            1 to "VK_NOT_READY",
            2 to "VK_TIMEOUT",
            3 to "VK_EVENT_SET",
            4 to "VK_EVENT_RESET",
            5 to "VK_INCOMPLETE",
            -1 to "VK_ERROR_OUT_OF_HOST_MEMORY",
            -2 to "VK_ERROR_OUT_OF_DEVICE_MEMORY",
            -3 to "VK_ERROR_INITIALIZATION_FAILED",
            -4 to "VK_ERROR_DEVICE_LOST",
            -5 to "VK_ERROR_MEMORY_MAP_FAILED",
            -6 to "VK_ERROR_LAYER_NOT_PRESENT",
            -7 to "VK_ERROR_EXTENSION_NOT_PRESENT",
            -8 to "VK_ERROR_FEATURE_NOT_PRESENT",
            -9 to "VK_ERROR_INCOMPATIBLE_DRIVER",
            -10 to "VK_ERROR_TOO_MANY_OBJECTS",
            -11 to "VK_ERROR_FORMAT_NOT_SUPPORTED",
            -12 to "VK_ERROR_FRAGMENTED_POOL",
            -13 to "VK_ERROR_UNKNOWN",
            1000001003 to "VK_SUBOPTIMAL_KHR",
            -1000001004 to "VK_ERROR_OUT_OF_DATE_KHR",
        )

    }

}

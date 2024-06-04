package dev.redstones.quasar.spvc

class SpvcException(message: String?, ret: Int? = null): Exception(transformMessage(message, ret)) {

    companion object {

        fun transformMessage(message: String?, ret: Int?): String {
            return if (ret != null) {
                "${map[ret] ?: ret.toString()}: $message"
            } else {
                message ?: "no error message"
            }
        }

        val map = mapOf(
            0 to "SPVC_SUCCESS",
            -1 to "SPVC_ERROR_INVALID_SPIRV",
            -2 to "SPVC_ERROR_UNSUPPORTED_SPIRV",
            -3 to "SPVC_ERROR_OUT_OF_MEMORY",
            -4 to "SPVC_ERROR_INVALID_ARGUMENT",
        )
    }

}
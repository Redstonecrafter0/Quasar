package net.redstonecraft.vulkan.spvc

class ShaderException(message: String?, ret: Int? = null): Exception(transformMessage(message, ret)) {

    companion object {

        fun transformMessage(message: String?, ret: Int?): String {
            return if (ret != null) {
                "${map[ret]}: $message"
            } else {
                message ?: "null"
            }
        }

        val map = mapOf(
            0 to "shaderc_compilation_status_success",
            1 to "shaderc_compilation_status_invalid_stage",
            2 to "shaderc_compilation_status_compilation_error",
            3 to "shaderc_compilation_status_internal_error",
            4 to "shaderc_compilation_status_null_result_object",
            5 to "shaderc_compilation_status_invalid_assembly",
            6 to "shaderc_compilation_status_validation_error",
            7 to "shaderc_compilation_status_transformation_error",
            8 to "shaderc_compilation_status_configuration_error"
        )
    }

}

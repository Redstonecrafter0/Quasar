package net.redstonecraft.vulkan.vfs

interface VirtualFileSystem {

    /**
     * @param fromFile Ignored if [file] is absolute. Otherwise [fromFile] must be an absolute path
     * */
    fun readFile(fromFile: String, file: String): String

    /**
     * @param absolutePath needs to start with '/'
     * */
    fun normalizePath(absolutePath: String): String {
        val trimmedPath = absolutePath.trim('/')
        val segments = mutableListOf<String>()

        for (i in trimmedPath.split("/")) {
            when (i) {
                "." -> {}
                ".." -> segments.removeLastOrNull()
                "" -> segments.clear()
                else -> segments += i
            }
        }
        return "/${segments.joinToString("/")}"
    }

    fun parent(path: String): String {
        return path.substring(0, path.lastIndexOf("/"))
    }


}

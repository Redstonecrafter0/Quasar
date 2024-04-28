package net.redstonecraft.vulkan.vfs

import java.io.FileNotFoundException

class ResourceVFS(root: String): VirtualFileSystem {

    val root = if (root.endsWith("/")) root.substring(0, root.length - 2) else root

    override fun readFile(fromFile: String, file: String): String {
        val path = if (file.startsWith("/")) {
            normalizePath(file)
        } else {
            "${parent(fromFile)}/$file"
        }
        val fullPath = "$root$path"
        val stream = javaClass.getResourceAsStream(fullPath) ?: throw FileNotFoundException("Resource not found: $fullPath")
        return stream.readAllBytes().decodeToString()
    }

}

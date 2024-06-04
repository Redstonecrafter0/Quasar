package dev.redstones.quasar.vfs

import java.io.File
import java.io.FileNotFoundException

class MappedVFS(root: String): VirtualFileSystem {

    private val canonicalRootPath = File(root).canonicalPath.run {
        if (endsWith("/")) {
            substring(0, lastIndex - 1)
        } else {
            this
        }
    }

    override fun readFile(fromFile: String, file: String): String {
        val path = if (file.startsWith("/")) {
            normalizePath(file)
        } else {
            "${parent(fromFile)}/$file"
        }
        val realFile = File("$canonicalRootPath$path")
        if (!realFile.canonicalPath.startsWith(canonicalRootPath)) {
            throw FileNotFoundException("Outside of root path $canonicalRootPath")
        }
        return realFile.readText()
    }

}

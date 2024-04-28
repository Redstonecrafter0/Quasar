package net.redstonecraft.vulkan.voxels

import net.redstonecraft.vulkan.util.SimplexNoise

class Chunk(val x: Int, val y: Int, val z: Int) {

    var vao = 0
    var vbo = 0
    var instanceVbo = 0
    var faceCount = 0

    private val blocks = ByteArray(4096) {
        val (bx, by, bz) = locFromIndex(it)
        val terrain = ((SimplexNoise.noise((x * 16 + bx) / 10.0, (z * 16 + bz) / 10.0) / 2 + 0.5) * 100 + 60).toInt()
        if (y * 16 + by <= terrain && SimplexNoise.noise((x * 16 + bx) / 15.0, (y * 16 + by) / 15.0, (z * 16 + bz) / 15.0) >= 0.5) {
            when (y * 16 + by) {
                terrain -> 1
                in (terrain - 3)..<terrain -> 2
                else -> 3
            }
        } else {
            0
        }
    }

    fun locFromIndex(index: Int): Triple<Int, Int, Int> {
        val x = index % 16
        val y = index / 16 % 16
        val z = index / 16 / 16 % 16
        return Triple(x, y, z)
    }

    fun locToIndex(loc: Triple<Int, Int, Int>) = locToIndex(loc.first, loc.second, loc.third)

    fun locToIndex(x: Int, y: Int, z: Int): Int {
        return x + (y * 16) + (z * 256)
    }

}

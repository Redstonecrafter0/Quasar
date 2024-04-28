package net.redstonecraft.vulkan.voxels

class World {

    val loadedChunks = mutableListOf<Chunk>()
    val ssbo: Int = 0 // size 12 * 16 * 12 * 4 * 3 = 27648 ^ max possible ssbo size in bytes

}

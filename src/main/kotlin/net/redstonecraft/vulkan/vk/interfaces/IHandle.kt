package net.redstonecraft.vulkan.vk.interfaces

import java.io.Closeable

interface IHandle<T>: Closeable {

    val handle: T

}

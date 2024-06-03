package net.redstonecraft.vulkan.interfaces

import java.io.Closeable

interface IHandle<T>: Closeable {

    val handle: T

}

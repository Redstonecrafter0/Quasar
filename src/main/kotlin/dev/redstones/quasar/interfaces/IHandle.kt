package dev.redstones.quasar.interfaces

import java.io.Closeable

interface IHandle<T>: Closeable {

    val handle: T

}

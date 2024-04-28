package net.redstonecraft.vulkan.util

import org.joml.Vector3f
import java.lang.Math.toRadians
import kotlin.math.cos
import kotlin.math.sin

fun Vector3f.rotate(yaw: Double, pitch: Double): Vector3f {
    x = (cos(toRadians(yaw)) * cos(toRadians(pitch))).toFloat()
    y = sin(toRadians(pitch)).toFloat()
    z = (sin(toRadians(yaw)) * cos(toRadians(pitch))).toFloat()
    return this.normalize()
}

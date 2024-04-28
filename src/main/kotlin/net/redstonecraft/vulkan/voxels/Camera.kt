package net.redstonecraft.vulkan.voxels

import net.redstonecraft.vulkan.height
import net.redstonecraft.vulkan.util.rotate
import net.redstonecraft.vulkan.width
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3f

class Camera {

    val pos = Vector3d(.0, 70.0, .0)
    var yaw = 30.0
    var pitch = 90.0
    val direction: Vector3f
        get() = Vector3f().rotate(yaw, pitch)
    val camera: Matrix4f
        get() = Matrix4f().setPerspective(Math.toRadians(100.0).toFloat(), width / height.toFloat(), 0.01F, 100F).lookAt(Vector3f(0F), direction, Vector3f(0F, 1F, 0F))

}

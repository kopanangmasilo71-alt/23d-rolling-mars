package com.example.engine

import android.opengl.Matrix

data class Vector3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
    fun set(nx: Float, ny: Float, nz: Float) {
        x = nx
        y = ny
        z = nz
    }

    fun distanceTo(other: Vector3): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun distanceSquared(other: Vector3): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return dx * dx + dy * dy + dz * dz
    }
}

class MatrixStack {
    private val stack = Array(16) { FloatArray(16) }
    private var top = 0

    init {
        Matrix.setIdentityM(stack[0], 0)
    }

    fun reset() {
        top = 0
        Matrix.setIdentityM(stack[0], 0)
    }

    fun get(): FloatArray = stack[top]

    fun push() {
        if (top < 15) {
            top++
            System.arraycopy(stack[top - 1], 0, stack[top], 0, 16)
        }
    }

    fun pop() {
        if (top > 0) {
            top--
        }
    }

    fun translate(x: Float, y: Float, z: Float) {
        Matrix.translateM(stack[top], 0, x, y, z)
    }

    fun rotate(angleDeg: Float, x: Float, y: Float, z: Float) {
        Matrix.rotateM(stack[top], 0, angleDeg, x, y, z)
    }

    fun scale(sx: Float, sy: Float, sz: Float) {
        Matrix.scaleM(stack[top], 0, sx, sy, sz)
    }
}

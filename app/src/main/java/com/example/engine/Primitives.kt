package com.example.engine

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class Mesh(
    val vertexBuffer: FloatBuffer,
    val indexBuffer: ShortBuffer,
    val indexCount: Int
)

object Primitives {
    const val POSITION_COMPONENT_COUNT = 3
    const val NORMAL_COMPONENT_COUNT = 3
    const val COLOR_COMPONENT_COUNT = 4
    const val TOTAL_COMPONENT_COUNT = 10
    const val STRIDE = TOTAL_COMPONENT_COUNT * 4 // 40 bytes per vertex

    private fun createFloatBuffer(data: FloatArray): FloatBuffer {
        return ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(data)
                position(0)
            }
    }

    private fun createShortBuffer(data: ShortArray): ShortBuffer {
        return ByteBuffer.allocateDirect(data.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply {
                put(data)
                position(0)
            }
    }

    /**
     * Builds a cube centered at (0, 0, 0) with distinct face normals.
     */
    fun createCube(
        w: Float = 1f,
        h: Float = 1f,
        d: Float = 1f,
        color: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)
    ): Mesh {
        val hw = w / 2f
        val hh = h / 2f
        val hd = d / 2f

        val vertices = ArrayList<Float>()
        val indices = ArrayList<Short>()

        fun addFace(
            p1: FloatArray, p2: FloatArray, p3: FloatArray, p4: FloatArray,
            norm: FloatArray, col: FloatArray
        ) {
            val baseIndex = (vertices.size / TOTAL_COMPONENT_COUNT).toShort()
            val pts = listOf(p1, p2, p3, p4)
            for (p in pts) {
                // Pos
                vertices.add(p[0])
                vertices.add(p[1])
                vertices.add(p[2])
                // Normal
                vertices.add(norm[0])
                vertices.add(norm[1])
                vertices.add(norm[2])
                // Color
                vertices.add(col[0])
                vertices.add(col[1])
                vertices.add(col[2])
                vertices.add(col[3])
            }
            indices.add(baseIndex)
            indices.add((baseIndex + 1).toShort())
            indices.add((baseIndex + 2).toShort())
            indices.add(baseIndex)
            indices.add((baseIndex + 2).toShort())
            indices.add((baseIndex + 3).toShort())
        }

        val r = color[0]
        val g = color[1]
        val b = color[2]
        val a = color[3]

        // Front face (+Z)
        addFace(
            floatArrayOf(-hw, -hh, hd), floatArrayOf(hw, -hh, hd),
            floatArrayOf(hw, hh, hd), floatArrayOf(-hw, hh, hd),
            floatArrayOf(0f, 0f, 1f), floatArrayOf(r, g, b, a)
        )
        // Back face (-Z)
        addFace(
            floatArrayOf(hw, -hh, -hd), floatArrayOf(-hw, -hh, -hd),
            floatArrayOf(-hw, hh, -hd), floatArrayOf(hw, hh, -hd),
            floatArrayOf(0f, 0f, -1f), floatArrayOf(r * 0.85f, g * 0.85f, b * 0.85f, a)
        )
        // Top face (+Y)
        addFace(
            floatArrayOf(-hw, hh, hd), floatArrayOf(hw, hh, hd),
            floatArrayOf(hw, hh, -hd), floatArrayOf(-hw, hh, -hd),
            floatArrayOf(0f, 1f, 0f), floatArrayOf(r * 1.15f.coerceAtMost(1f), g * 1.15f.coerceAtMost(1f), b * 1.15f.coerceAtMost(1f), a)
        )
        // Bottom face (-Y)
        addFace(
            floatArrayOf(-hw, -hh, -hd), floatArrayOf(hw, -hh, -hd),
            floatArrayOf(hw, -hh, hd), floatArrayOf(-hw, -hh, hd),
            floatArrayOf(0f, -1f, 0f), floatArrayOf(r * 0.7f, g * 0.7f, b * 0.7f, a)
        )
        // Right face (+X)
        addFace(
            floatArrayOf(hw, -hh, hd), floatArrayOf(hw, -hh, -hd),
            floatArrayOf(hw, hh, -hd), floatArrayOf(hw, hh, hd),
            floatArrayOf(1f, 0f, 0f), floatArrayOf(r * 0.95f, g * 0.95f, b * 0.95f, a)
        )
        // Left face (-X)
        addFace(
            floatArrayOf(-hw, -hh, -hd), floatArrayOf(-hw, -hh, hd),
            floatArrayOf(-hw, hh, hd), floatArrayOf(-hw, hh, -hd),
            floatArrayOf(-1f, 0f, 0f), floatArrayOf(r * 0.8f, g * 0.8f, b * 0.8f, a)
        )

        return Mesh(
            createFloatBuffer(vertices.toFloatArray()),
            createShortBuffer(indices.toShortArray()),
            indices.size
        )
    }

    /**
     * UV Sphere with alternating colored latitude/longitude pattern so rotation is clearly visible.
     */
    fun createRollingSphere(
        radius: Float = 1f,
        rings: Int = 14,
        sectors: Int = 18,
        colorA: FloatArray = floatArrayOf(0.95f, 0.25f, 0.2f, 1f),
        colorB: FloatArray = floatArrayOf(0.98f, 0.85f, 0.2f, 1f)
    ): Mesh {
        val vertices = ArrayList<Float>()
        val indices = ArrayList<Short>()

        val R = 1f / (rings - 1).toFloat()
        val S = 1f / (sectors - 1).toFloat()

        for (r in 0 until rings) {
            val v = r * R
            val theta = v * PI.toFloat()
            val sinTheta = sin(theta)
            val cosTheta = cos(theta)

            for (s in 0 until sectors) {
                val u = s * S
                val phi = u * 2f * PI.toFloat()
                val sinPhi = sin(phi)
                val cosPhi = cos(phi)

                val x = cosPhi * sinTheta
                val y = cosTheta
                val z = sinPhi * sinTheta

                // Alternating checker/stripe pattern to emphasize rolling rotation
                val isStripe = ((r / 2 + s / 2) % 2 == 0)
                val col = if (isStripe) colorA else colorB

                // Pos
                vertices.add(x * radius)
                vertices.add(y * radius)
                vertices.add(z * radius)
                // Normal
                vertices.add(x)
                vertices.add(y)
                vertices.add(z)
                // Color
                vertices.add(col[0])
                vertices.add(col[1])
                vertices.add(col[2])
                vertices.add(col[3])
            }
        }

        for (r in 0 until rings - 1) {
            for (s in 0 until sectors - 1) {
                val i0 = (r * sectors + s).toShort()
                val i1 = (r * sectors + (s + 1)).toShort()
                val i2 = ((r + 1) * sectors + (s + 1)).toShort()
                val i3 = ((r + 1) * sectors + s).toShort()

                indices.add(i0)
                indices.add(i1)
                indices.add(i2)
                indices.add(i0)
                indices.add(i2)
                indices.add(i3)
            }
        }

        return Mesh(
            createFloatBuffer(vertices.toFloatArray()),
            createShortBuffer(indices.toShortArray()),
            indices.size
        )
    }

    /**
     * Cylinder for tree trunks, obstacles, and barriers.
     */
    fun createCylinder(
        radius: Float = 0.5f,
        height: Float = 2f,
        segments: Int = 12,
        color: FloatArray = floatArrayOf(0.45f, 0.28f, 0.16f, 1f)
    ): Mesh {
        val vertices = ArrayList<Float>()
        val indices = ArrayList<Short>()
        val halfH = height / 2f

        for (i in 0..segments) {
            val angle = (i.toFloat() / segments) * 2f * PI.toFloat()
            val x = cos(angle)
            val z = sin(angle)

            // Bottom vertex
            vertices.add(x * radius); vertices.add(-halfH); vertices.add(z * radius)
            vertices.add(x); vertices.add(0f); vertices.add(z)
            vertices.add(color[0] * 0.8f); vertices.add(color[1] * 0.8f); vertices.add(color[2] * 0.8f); vertices.add(color[3])

            // Top vertex
            vertices.add(x * radius); vertices.add(halfH); vertices.add(z * radius)
            vertices.add(x); vertices.add(0f); vertices.add(z)
            vertices.add(color[0]); vertices.add(color[1]); vertices.add(color[2]); vertices.add(color[3])
        }

        for (i in 0 until segments) {
            val b1 = (i * 2).toShort()
            val t1 = (i * 2 + 1).toShort()
            val b2 = ((i + 1) * 2).toShort()
            val t2 = ((i + 1) * 2 + 1).toShort()

            indices.add(b1); indices.add(t1); indices.add(t2)
            indices.add(b1); indices.add(t2); indices.add(b2)
        }

        return Mesh(
            createFloatBuffer(vertices.toFloatArray()),
            createShortBuffer(indices.toShortArray()),
            indices.size
        )
    }

    /**
     * Quad for road segments and markings on XZ plane, facing up (+Y).
     */
    fun createPlane(
        width: Float = 1f,
        length: Float = 1f,
        color: FloatArray = floatArrayOf(0.2f, 0.2f, 0.24f, 1f)
    ): Mesh {
        val hw = width / 2f
        val hl = length / 2f

        val vertices = floatArrayOf(
            -hw, 0f,  hl,   0f, 1f, 0f,   color[0], color[1], color[2], color[3],
             hw, 0f,  hl,   0f, 1f, 0f,   color[0], color[1], color[2], color[3],
             hw, 0f, -hl,   0f, 1f, 0f,   color[0], color[1], color[2], color[3],
            -hw, 0f, -hl,   0f, 1f, 0f,   color[0], color[1], color[2], color[3]
        )
        val indices = shortArrayOf(0, 1, 2, 0, 2, 3)

        return Mesh(
            createFloatBuffer(vertices),
            createShortBuffer(indices),
            indices.size
        )
    }

    /**
     * Smooth organic UV Sphere with radial normals and continuous lighting.
     */
    fun createSmoothSphere(
        radius: Float = 1f,
        rings: Int = 16,
        sectors: Int = 20,
        color: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)
    ): Mesh {
        val vertices = ArrayList<Float>()
        val indices = ArrayList<Short>()

        val R = 1f / (rings - 1).toFloat()
        val S = 1f / (sectors - 1).toFloat()

        for (r in 0 until rings) {
            val v = r * R
            val theta = v * PI.toFloat()
            val sinTheta = sin(theta)
            val cosTheta = cos(theta)

            for (s in 0 until sectors) {
                val u = s * S
                val phi = u * 2f * PI.toFloat()
                val sinPhi = sin(phi)
                val cosPhi = cos(phi)

                val x = cosPhi * sinTheta
                val y = cosTheta
                val z = sinPhi * sinTheta

                // Pos
                vertices.add(x * radius)
                vertices.add(y * radius)
                vertices.add(z * radius)
                // Normal
                vertices.add(x)
                vertices.add(y)
                vertices.add(z)
                // Color
                vertices.add(color[0])
                vertices.add(color[1])
                vertices.add(color[2])
                vertices.add(color[3])
            }
        }

        for (r in 0 until rings - 1) {
            for (s in 0 until sectors - 1) {
                val i0 = (r * sectors + s).toShort()
                val i1 = (r * sectors + (s + 1)).toShort()
                val i2 = ((r + 1) * sectors + (s + 1)).toShort()
                val i3 = ((r + 1) * sectors + s).toShort()

                indices.add(i0)
                indices.add(i1)
                indices.add(i2)
                indices.add(i0)
                indices.add(i2)
                indices.add(i3)
            }
        }

        return Mesh(
            createFloatBuffer(vertices.toFloatArray()),
            createShortBuffer(indices.toShortArray()),
            indices.size
        )
    }

    /**
     * Glowing Energy Torus / Ring for halos, checkpoints, and cyber arches.
     */
    fun createTorusRing(
        majorRadius: Float = 1.2f,
        minorRadius: Float = 0.12f,
        majorSegments: Int = 20,
        minorSegments: Int = 10,
        color: FloatArray = floatArrayOf(0f, 0.95f, 1f, 1f)
    ): Mesh {
        val vertices = ArrayList<Float>()
        val indices = ArrayList<Short>()

        for (i in 0 until majorSegments) {
            val u = (i.toFloat() / majorSegments) * 2f * PI.toFloat()
            val cosU = cos(u)
            val sinU = sin(u)

            for (j in 0 until minorSegments) {
                val v = (j.toFloat() / minorSegments) * 2f * PI.toFloat()
                val cosV = cos(v)
                val sinV = sin(v)

                val x = (majorRadius + minorRadius * cosV) * cosU
                val y = minorRadius * sinV
                val z = (majorRadius + minorRadius * cosV) * sinU

                val nx = cosV * cosU
                val ny = sinV
                val nz = cosV * sinU

                vertices.add(x); vertices.add(y); vertices.add(z)
                vertices.add(nx); vertices.add(ny); vertices.add(nz)
                vertices.add(color[0]); vertices.add(color[1]); vertices.add(color[2]); vertices.add(color[3])
            }
        }

        for (i in 0 until majorSegments) {
            val nextI = (i + 1) % majorSegments
            for (j in 0 until minorSegments) {
                val nextJ = (j + 1) % minorSegments

                val i0 = (i * minorSegments + j).toShort()
                val i1 = (nextI * minorSegments + j).toShort()
                val i2 = (nextI * minorSegments + nextJ).toShort()
                val i3 = (i * minorSegments + nextJ).toShort()

                indices.add(i0); indices.add(i1); indices.add(i2)
                indices.add(i0); indices.add(i2); indices.add(i3)
            }
        }

        return Mesh(
            createFloatBuffer(vertices.toFloatArray()),
            createShortBuffer(indices.toShortArray()),
            indices.size
        )
    }

    /**
     * Panoramic Horizon Backdrop with layered mountains and atmospheric horizon glow.
     */
    fun createSkyBackdrop(
        radius: Float = 95f,
        height: Float = 45f,
        segments: Int = 24
    ): Mesh {
        val vertices = ArrayList<Float>()
        val indices = ArrayList<Short>()

        val topColor = floatArrayOf(0.04f, 0.06f, 0.16f, 1f) // Deep midnight space
        val midColor = floatArrayOf(0.12f, 0.10f, 0.28f, 1f) // Twilight nebula violet
        val horizonColor = floatArrayOf(0.95f, 0.40f, 0.20f, 1f) // Radiant dusk horizon amber

        for (i in 0..segments) {
            val angle = (i.toFloat() / segments) * PI.toFloat() + (PI.toFloat() / 2f) // Semi-circle ahead
            val x = cos(angle) * radius
            val z = sin(angle) * radius

            // Base vertex (horizon)
            vertices.add(x); vertices.add(0f); vertices.add(z)
            vertices.add(-cos(angle)); vertices.add(0f); vertices.add(-sin(angle))
            vertices.add(horizonColor[0]); vertices.add(horizonColor[1]); vertices.add(horizonColor[2]); vertices.add(1f)

            // Mid vertex (mountain peaks)
            val peakH = 10f + sin(i * 1.5f) * 4f
            vertices.add(x * 0.98f); vertices.add(peakH); vertices.add(z * 0.98f)
            vertices.add(-cos(angle)); vertices.add(0.2f); vertices.add(-sin(angle))
            vertices.add(midColor[0]); vertices.add(midColor[1]); vertices.add(midColor[2]); vertices.add(1f)

            // Top sky vertex
            vertices.add(x * 0.9f); vertices.add(height); vertices.add(z * 0.9f)
            vertices.add(-cos(angle)); vertices.add(0.5f); vertices.add(-sin(angle))
            vertices.add(topColor[0]); vertices.add(topColor[1]); vertices.add(topColor[2]); vertices.add(1f)
        }

        for (i in 0 until segments) {
            val b1 = (i * 3).toShort()
            val m1 = (i * 3 + 1).toShort()
            val t1 = (i * 3 + 2).toShort()

            val b2 = ((i + 1) * 3).toShort()
            val m2 = ((i + 1) * 3 + 1).toShort()
            val t2 = ((i + 1) * 3 + 2).toShort()

            // Lower mountain quad
            indices.add(b1); indices.add(m1); indices.add(m2)
            indices.add(b1); indices.add(m2); indices.add(b2)

            // Upper sky quad
            indices.add(m1); indices.add(t1); indices.add(t2)
            indices.add(m1); indices.add(t2); indices.add(m2)
        }

        return Mesh(
            createFloatBuffer(vertices.toFloatArray()),
            createShortBuffer(indices.toShortArray()),
            indices.size
        )
    }
}

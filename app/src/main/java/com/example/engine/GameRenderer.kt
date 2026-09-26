package com.example.engine

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GameRenderer(val physics: GamePhysicsEngine) : GLSurfaceView.Renderer {

    // Inputs managed from Compose UI
    @Volatile var isLeftHeld: Boolean = false
    @Volatile var isRightHeld: Boolean = false
    @Volatile var isBrakeHeld: Boolean = false

    private var programId = 0
    private var uMVPMatrixLoc = 0
    private var uModelMatrixLoc = 0
    private var uColorOverrideLoc = 0
    private var uCamPosLoc = 0
    private var uLightDirLoc = 0
    private var uLightColorLoc = 0
    private var uAmbientColorLoc = 0
    private var uFogColorLoc = 0
    private var uFogHorizonColorLoc = 0
    private var uFogStartLoc = 0
    private var uFogEndLoc = 0
    private var uEmissiveLoc = 0
    private var aPositionLoc = 0
    private var aNormalLoc = 0
    private var aColorLoc = 0

    // Matrices
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val matrixStack = MatrixStack()

    // Meshes
    private var isReady: Boolean = false
    private lateinit var cubeMesh: Mesh
    private lateinit var roadQuadMesh: Mesh
    private lateinit var grassQuadMesh: Mesh
    private lateinit var cylinderMesh: Mesh
    private lateinit var smoothSphereMesh: Mesh
    private lateinit var torusRingMesh: Mesh
    private lateinit var skyBackdropMesh: Mesh

    // Ball Meshes
    private lateinit var straightBallMesh: Mesh
    private lateinit var crosserRightBallMesh: Mesh
    private lateinit var crosserLeftBallMesh: Mesh
    private lateinit var fastBallMesh: Mesh
    private lateinit var giantBallMesh: Mesh
    private lateinit var bouncerBallMesh: Mesh

    private var lastTimeNanos: Long = 0L

    private val vertexShaderSource = """
        uniform mat4 uMVPMatrix;
        uniform mat4 uModelMatrix;
        uniform vec4 uColorOverride;
        attribute vec3 aPosition;
        attribute vec3 aNormal;
        attribute vec4 aColor;

        varying vec3 vNormal;
        varying vec3 vPosition;
        varying vec4 vColor;

        void main() {
            vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
            vPosition = worldPos.xyz;
            mat3 normalMatrix = mat3(
                uModelMatrix[0].x, uModelMatrix[0].y, uModelMatrix[0].z,
                uModelMatrix[1].x, uModelMatrix[1].y, uModelMatrix[1].z,
                uModelMatrix[2].x, uModelMatrix[2].y, uModelMatrix[2].z
            );
            vNormal = normalize(normalMatrix * aNormal);
            if (uColorOverride.a > 0.0) {
                vColor = uColorOverride;
            } else {
                vColor = aColor;
            }
            gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
        }
    """.trimIndent()

    private val fragmentShaderSource = """
        precision mediump float;
        uniform vec3 uCamPos;
        uniform vec3 uLightDir;
        uniform vec3 uLightColor;
        uniform vec3 uAmbientColor;
        uniform vec3 uFogColor;
        uniform vec3 uFogHorizonColor;
        uniform float uFogStart;
        uniform float uFogEnd;
        uniform float uEmissive;

        varying vec3 vNormal;
        varying vec3 vPosition;
        varying vec4 vColor;

        void main() {
            vec3 norm = normalize(vNormal);
            vec3 lightDir = normalize(uLightDir);
            vec3 viewDir = normalize(uCamPos - vPosition);

            // Diffuse Lighting (Half-Lambert for soft, natural lighting on curves)
            float NdotL = max(dot(norm, lightDir), 0.0);
            float halfLambert = NdotL * 0.70 + 0.30;
            vec3 diffuse = uLightColor * halfLambert;

            // Blinn-Phong Specular Highlight (AAA metallic / glossy sheen)
            vec3 halfVector = normalize(lightDir + viewDir);
            float specFactor = pow(max(dot(norm, halfVector), 0.0), 32.0);
            vec3 specular = uLightColor * (specFactor * 0.50);

            // Fresnel Rim Lighting (Crisp cinematic edge glow)
            float rimFactor = pow(1.0 - max(dot(norm, viewDir), 0.0), 3.0);
            vec3 rimLight = vec3(0.35, 0.75, 1.0) * (rimFactor * 0.50);

            // Combined Surface Lighting
            vec3 surfaceLighting = uAmbientColor + diffuse + specular + rimLight;
            vec3 litColor = vColor.rgb * surfaceLighting;

            // Emissive Glow Boost (for neon strips, visors, lamps, magma cores)
            if (uEmissive > 0.0) {
                litColor = mix(litColor, vColor.rgb * 1.5, uEmissive);
            }

            // Dual-Gradient Atmospheric Depth Fog
            float dist = length(vPosition - uCamPos);
            float fogFactor = clamp((dist - uFogStart) / (uFogEnd - uFogStart), 0.0, 1.0);
            vec3 skyAtmosphere = mix(uFogColor, uFogHorizonColor, clamp((vPosition.y + 2.0) * 0.06, 0.0, 1.0));
            vec3 finalColor = mix(litColor, skyAtmosphere, fogFactor * fogFactor);

            gl_FragColor = vec4(finalColor, vColor.a);
        }
    """.trimIndent()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        try {
            isReady = false
            // Deep neon night / twilight horizon sky
            GLES20.glClearColor(0.06f, 0.08f, 0.16f, 1.0f)
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            GLES20.glDepthFunc(GLES20.GL_LEQUAL)
            GLES20.glEnable(GLES20.GL_CULL_FACE)
            GLES20.glCullFace(GLES20.GL_BACK)

            programId = ShaderUtil.createProgram(vertexShaderSource, fragmentShaderSource)
            if (programId == 0) {
                android.util.Log.e("GameRenderer", "Failed to compile/link AAA shader program")
                return
            }

            uMVPMatrixLoc = GLES20.glGetUniformLocation(programId, "uMVPMatrix")
            uModelMatrixLoc = GLES20.glGetUniformLocation(programId, "uModelMatrix")
            uColorOverrideLoc = GLES20.glGetUniformLocation(programId, "uColorOverride")
            uCamPosLoc = GLES20.glGetUniformLocation(programId, "uCamPos")
            uLightDirLoc = GLES20.glGetUniformLocation(programId, "uLightDir")
            uLightColorLoc = GLES20.glGetUniformLocation(programId, "uLightColor")
            uAmbientColorLoc = GLES20.glGetUniformLocation(programId, "uAmbientColor")
            uFogColorLoc = GLES20.glGetUniformLocation(programId, "uFogColor")
            uFogHorizonColorLoc = GLES20.glGetUniformLocation(programId, "uFogHorizonColor")
            uFogStartLoc = GLES20.glGetUniformLocation(programId, "uFogStart")
            uFogEndLoc = GLES20.glGetUniformLocation(programId, "uFogEnd")
            uEmissiveLoc = GLES20.glGetUniformLocation(programId, "uEmissive")

            aPositionLoc = GLES20.glGetAttribLocation(programId, "aPosition")
            aNormalLoc = GLES20.glGetAttribLocation(programId, "aNormal")
            aColorLoc = GLES20.glGetAttribLocation(programId, "aColor")

            // Initialize high-detail geometric meshes
            cubeMesh = Primitives.createCube(1f, 1f, 1f, floatArrayOf(1f, 1f, 1f, 1f))
            roadQuadMesh = Primitives.createPlane(physics.roadWidth, physics.segmentLength, floatArrayOf(0.12f, 0.14f, 0.18f, 1f))
            grassQuadMesh = Primitives.createPlane(45f, physics.segmentLength, floatArrayOf(0.08f, 0.22f, 0.14f, 1f))
            cylinderMesh = Primitives.createCylinder(0.35f, 2.0f, 14, floatArrayOf(0.38f, 0.24f, 0.15f, 1f))
            smoothSphereMesh = Primitives.createSmoothSphere(1f, 16, 20, floatArrayOf(1f, 1f, 1f, 1f))
            torusRingMesh = Primitives.createTorusRing(1.6f, 0.12f, 22, 10, floatArrayOf(0f, 0.95f, 1f, 1f))
            skyBackdropMesh = Primitives.createSkyBackdrop(110f, 50f, 24)

            // Dynamic Sphere Obstacle Meshes
            straightBallMesh = Primitives.createRollingSphere(
                radius = BallType.STRAIGHT.radius,
                colorA = floatArrayOf(0.96f, 0.38f, 0.10f, 1f),
                colorB = floatArrayOf(1.0f, 0.88f, 0.18f, 1f)
            )
            crosserRightBallMesh = Primitives.createRollingSphere(
                radius = BallType.LEFT_TO_RIGHT.radius,
                colorA = floatArrayOf(0.05f, 0.75f, 0.98f, 1f),
                colorB = floatArrayOf(0.90f, 0.98f, 1.0f, 1f)
            )
            crosserLeftBallMesh = Primitives.createRollingSphere(
                radius = BallType.RIGHT_TO_LEFT.radius,
                colorA = floatArrayOf(0.88f, 0.12f, 0.88f, 1f),
                colorB = floatArrayOf(1.0f, 0.75f, 0.98f, 1f)
            )
            fastBallMesh = Primitives.createRollingSphere(
                radius = BallType.FAST.radius,
                colorA = floatArrayOf(1.0f, 0.05f, 0.10f, 1f),
                colorB = floatArrayOf(1.0f, 0.95f, 0.05f, 1f)
            )
            giantBallMesh = Primitives.createRollingSphere(
                radius = BallType.GIANT.radius,
                colorA = floatArrayOf(0.25f, 0.28f, 0.38f, 1f),
                colorB = floatArrayOf(0.85f, 0.45f, 0.15f, 1f)
            )
            bouncerBallMesh = Primitives.createRollingSphere(
                radius = BallType.BOUNCING.radius,
                colorA = floatArrayOf(0.15f, 0.90f, 0.35f, 1f),
                colorB = floatArrayOf(0.85f, 1.0f, 0.20f, 1f)
            )

            lastTimeNanos = System.nanoTime()
            isReady = true
        } catch (t: Throwable) {
            android.util.Log.e("GameRenderer", "Error during onSurfaceCreated", t)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        try {
            GLES20.glViewport(0, 0, width, height)
            val h = if (height > 0) height else 1
            val ratio = width.toFloat() / h.toFloat()
            // Immersive 64-degree perspective camera
            Matrix.perspectiveM(projectionMatrix, 0, 64f, ratio, 0.5f, 160f)
        } catch (t: Throwable) {
            android.util.Log.e("GameRenderer", "Error during onSurfaceChanged", t)
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        try {
            val now = System.nanoTime()
            val dt = if (lastTimeNanos != 0L) ((now - lastTimeNanos) / 1_000_000_000.0f).coerceIn(0.001f, 0.05f) else 0.016f
            lastTimeNanos = now

            // Update physics step
            physics.update(dt, isLeftHeld, isRightHeld, isBrakeHeld)

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            if (!isReady || programId == 0) return

            GLES20.glUseProgram(programId)

            // Setup View Matrix
            val cam = physics.cameraPos
            val target = physics.cameraLookAt
            Matrix.setLookAtM(
                viewMatrix, 0,
                cam.x, cam.y, cam.z,
                target.x, target.y, target.z,
                0f, 1f, 0f
            )

            // Camera World Position for specular/rim lighting
            GLES20.glUniform3f(uCamPosLoc, cam.x, cam.y, cam.z)

            // Directional Sun/Sky Light + Ambient
            GLES20.glUniform3f(uLightDirLoc, 0.55f, 0.85f, 0.35f)
            GLES20.glUniform3f(uLightColorLoc, 0.90f, 0.88f, 0.82f)
            GLES20.glUniform3f(uAmbientColorLoc, 0.38f, 0.40f, 0.48f)

            // Dual-Gradient Atmospheric Depth Fog (Twilight Blue -> Horizon Amber)
            GLES20.glUniform3f(uFogColorLoc, 0.06f, 0.08f, 0.16f)
            GLES20.glUniform3f(uFogHorizonColorLoc, 0.85f, 0.38f, 0.15f)
            GLES20.glUniform1f(uFogStartLoc, 35f)
            GLES20.glUniform1f(uFogEndLoc, 105f)

            matrixStack.reset()

            // 1. Panoramic Sky Backdrop & Mountains
            renderSkyBackdrop()

            // 2. High-Tech Speedway & Overhead Cyber Gates
            renderEnvironment()

            // 3. Roadside Scenery (Cyber Pylons, Trees, Rocks)
            renderScenery()

            // 4. Rolling Boulders with Emissive Energy Halos
            renderBalls()

            // 5. Realistic Articulated Hero Runner
            renderPlayer()

            // 6. Particle Energy & Sparks
            renderParticles()
        } catch (t: Throwable) {
            android.util.Log.e("GameRenderer", "Error in onDrawFrame", t)
        }
    }

    private fun drawMesh(mesh: Mesh, colorOverride: FloatArray? = null, emissive: Float = 0f) {
        if (!isReady || programId == 0 || aPositionLoc < 0 || aNormalLoc < 0 || aColorLoc < 0) return

        val model = matrixStack.get()
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, model, 0)

        GLES20.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelMatrixLoc, 1, false, model, 0)
        GLES20.glUniform1f(uEmissiveLoc, emissive)

        if (colorOverride != null) {
            GLES20.glUniform4fv(uColorOverrideLoc, 1, colorOverride, 0)
        } else {
            GLES20.glUniform4f(uColorOverrideLoc, 0f, 0f, 0f, 0f)
        }

        mesh.vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(aPositionLoc, Primitives.POSITION_COMPONENT_COUNT, GLES20.GL_FLOAT, false, Primitives.STRIDE, mesh.vertexBuffer)
        GLES20.glEnableVertexAttribArray(aPositionLoc)

        mesh.vertexBuffer.position(Primitives.POSITION_COMPONENT_COUNT)
        GLES20.glVertexAttribPointer(aNormalLoc, Primitives.NORMAL_COMPONENT_COUNT, GLES20.GL_FLOAT, false, Primitives.STRIDE, mesh.vertexBuffer)
        GLES20.glEnableVertexAttribArray(aNormalLoc)

        mesh.vertexBuffer.position(Primitives.POSITION_COMPONENT_COUNT + Primitives.NORMAL_COMPONENT_COUNT)
        GLES20.glVertexAttribPointer(aColorLoc, Primitives.COLOR_COMPONENT_COUNT, GLES20.GL_FLOAT, false, Primitives.STRIDE, mesh.vertexBuffer)
        GLES20.glEnableVertexAttribArray(aColorLoc)

        mesh.indexBuffer.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mesh.indexCount, GLES20.GL_UNSIGNED_SHORT, mesh.indexBuffer)
    }

    private fun renderSkyBackdrop() {
        val cam = physics.cameraPos
        matrixStack.push()
        // Center sky dome on camera so mountains and horizon stay majestic
        matrixStack.translate(cam.x, -5f, cam.z)
        drawMesh(skyBackdropMesh, emissive = 0.35f)

        // Radiant Celestial Sun / Nebula Core on horizon
        matrixStack.push()
        matrixStack.translate(0f, 18f, -90f)
        matrixStack.scale(12f, 12f, 0.1f)
        drawMesh(smoothSphereMesh, floatArrayOf(1.0f, 0.65f, 0.20f, 1f), emissive = 0.85f)
        matrixStack.pop()

        matrixStack.pop()
    }

    private fun renderEnvironment() {
        val halfW = physics.roadHalfWidth
        val segLen = physics.segmentLength

        for (seg in physics.roadSegments) {
            val centerZ = seg.zStart - segLen / 2f

            // Asphalt Speedway Surface
            matrixStack.push()
            matrixStack.translate(0f, 0f, centerZ)
            drawMesh(roadQuadMesh, floatArrayOf(0.12f, 0.14f, 0.18f, 1f))
            matrixStack.pop()

            // Left & Right Grass/Terrain Banks
            matrixStack.push()
            matrixStack.translate(-(halfW + 22.5f), -0.02f, centerZ)
            drawMesh(grassQuadMesh, floatArrayOf(0.08f, 0.20f, 0.14f, 1f))
            matrixStack.pop()

            matrixStack.push()
            matrixStack.translate(halfW + 22.5f, -0.02f, centerZ)
            drawMesh(grassQuadMesh, floatArrayOf(0.08f, 0.20f, 0.14f, 1f))
            matrixStack.pop()

            // Curbs: alternating red/white safety barriers with metallic top
            matrixStack.push()
            matrixStack.translate(-halfW, 0.16f, centerZ)
            matrixStack.scale(0.38f, 0.32f, segLen)
            drawMesh(cubeMesh, floatArrayOf(0.95f, 0.22f, 0.18f, 1f))
            matrixStack.pop()

            matrixStack.push()
            matrixStack.translate(halfW, 0.16f, centerZ)
            matrixStack.scale(0.38f, 0.32f, segLen)
            drawMesh(cubeMesh, floatArrayOf(0.95f, 0.22f, 0.18f, 1f))
            matrixStack.pop()

            // Glowing Center Dashed Lines (Reflective Cyber Strips)
            val dashes = 6
            val dashStep = segLen / dashes
            for (d in 0 until dashes) {
                val dz = seg.zStart - (d + 0.5f) * dashStep
                matrixStack.push()
                matrixStack.translate(0f, 0.025f, dz)
                matrixStack.scale(0.24f, 0.02f, 2.5f)
                drawMesh(cubeMesh, floatArrayOf(0.00f, 0.90f, 1.00f, 1f), emissive = 0.65f)
                matrixStack.pop()
            }

            // Overhead Cyber Speedway Gate spanning the track
            matrixStack.push()
            matrixStack.translate(0f, 0f, seg.zStart)

            // Left Post
            matrixStack.push()
            matrixStack.translate(-halfW - 0.4f, 3.2f, 0f)
            matrixStack.scale(0.4f, 6.4f, 0.4f)
            drawMesh(cubeMesh, floatArrayOf(0.20f, 0.24f, 0.32f, 1f))
            matrixStack.pop()

            // Right Post
            matrixStack.push()
            matrixStack.translate(halfW + 0.4f, 3.2f, 0f)
            matrixStack.scale(0.4f, 6.4f, 0.4f)
            drawMesh(cubeMesh, floatArrayOf(0.20f, 0.24f, 0.32f, 1f))
            matrixStack.pop()

            // Overhead Crossbar with glowing chevron sign
            matrixStack.push()
            matrixStack.translate(0f, 6.2f, 0f)
            matrixStack.scale(physics.roadWidth + 1.2f, 0.5f, 0.6f)
            drawMesh(cubeMesh, floatArrayOf(0.15f, 0.18f, 0.24f, 1f))
            matrixStack.pop()

            // Glowing neon energy bar on crossbar
            matrixStack.push()
            matrixStack.translate(0f, 6.2f, -0.32f)
            matrixStack.scale(physics.roadWidth * 0.85f, 0.18f, 0.05f)
            drawMesh(cubeMesh, floatArrayOf(0.00f, 0.95f, 1.00f, 1f), emissive = 0.95f)
            matrixStack.pop()

            matrixStack.pop()
        }
    }

    private fun renderScenery() {
        for (item in physics.scenery) {
            matrixStack.push()
            matrixStack.translate(item.x, 0f, item.z)
            matrixStack.rotate(item.rotationY, 0f, 1f, 0f)
            matrixStack.scale(item.scale, item.scale, item.scale)

            when (item.type) {
                0 -> {
                    // Pine Tree: trunk + stylized conical canopy
                    matrixStack.push()
                    matrixStack.translate(0f, 1.0f, 0f)
                    matrixStack.scale(0.5f, 2.0f, 0.5f)
                    drawMesh(cubeMesh, floatArrayOf(0.35f, 0.22f, 0.14f, 1f))
                    matrixStack.pop()

                    matrixStack.push()
                    matrixStack.translate(0f, 2.5f, 0f)
                    matrixStack.scale(2.4f, 1.8f, 2.4f)
                    drawMesh(cubeMesh, floatArrayOf(0.10f, 0.42f, 0.22f, 1f))
                    matrixStack.pop()

                    matrixStack.push()
                    matrixStack.translate(0f, 3.8f, 0f)
                    matrixStack.scale(1.6f, 1.6f, 1.6f)
                    drawMesh(cubeMesh, floatArrayOf(0.14f, 0.52f, 0.26f, 1f))
                    matrixStack.pop()
                }
                1 -> {
                    // Cyber Light Tower: tall pylon with glowing vertical LED lightbar
                    matrixStack.push()
                    matrixStack.translate(0f, 3.0f, 0f)
                    matrixStack.scale(0.35f, 6.0f, 0.35f)
                    drawMesh(cubeMesh, floatArrayOf(0.20f, 0.24f, 0.32f, 1f))
                    matrixStack.pop()

                    // Angled light head
                    matrixStack.push()
                    matrixStack.translate(if (item.x < 0) 0.6f else -0.6f, 6.0f, 0f)
                    matrixStack.scale(1.2f, 0.25f, 0.5f)
                    drawMesh(cubeMesh, floatArrayOf(0.25f, 0.28f, 0.38f, 1f))
                    matrixStack.pop()

                    // Glowing LED lens
                    matrixStack.push()
                    matrixStack.translate(if (item.x < 0) 0.6f else -0.6f, 5.85f, 0f)
                    matrixStack.scale(1.0f, 0.08f, 0.4f)
                    drawMesh(cubeMesh, floatArrayOf(0.00f, 0.95f, 1.00f, 1f), emissive = 0.95f)
                    matrixStack.pop()
                }
                2 -> {
                    // Rock Monolith Boulder
                    matrixStack.push()
                    matrixStack.translate(0f, 0.8f, 0f)
                    matrixStack.rotate(32f, 1f, 0f, 1f)
                    matrixStack.scale(2.0f, 1.5f, 1.8f)
                    drawMesh(cubeMesh, floatArrayOf(0.42f, 0.44f, 0.48f, 1f))
                    matrixStack.pop()
                }
            }
            matrixStack.pop()
        }
    }

    private fun renderBalls() {
        for (ball in physics.ballPool) {
            if (!ball.isActive) continue

            matrixStack.push()
            matrixStack.translate(ball.position.x, ball.position.y, ball.position.z)
            matrixStack.rotate(ball.rollAngleX, 1f, 0f, 0f)
            matrixStack.rotate(ball.rollAngleZ, 0f, 0f, 1f)

            val mesh = when (ball.ballType) {
                BallType.STRAIGHT -> straightBallMesh
                BallType.LEFT_TO_RIGHT -> crosserRightBallMesh
                BallType.RIGHT_TO_LEFT -> crosserLeftBallMesh
                BallType.FAST -> fastBallMesh
                BallType.GIANT -> giantBallMesh
                BallType.BOUNCING -> bouncerBallMesh
            }
            val emissiveBoost = if (ball.ballType == BallType.FAST) 0.35f else 0.08f
            drawMesh(mesh, emissive = emissiveBoost)

            // For Juggernaut, draw an orbiting glowing energy halo ring
            if (ball.ballType == BallType.GIANT) {
                matrixStack.push()
                matrixStack.scale(1.15f, 1.15f, 1.15f)
                drawMesh(torusRingMesh, floatArrayOf(1.0f, 0.45f, 0.10f, 1f), emissive = 0.75f)
                matrixStack.pop()
            } else if (ball.ballType == BallType.BOUNCING) {
                matrixStack.push()
                matrixStack.scale(0.85f, 0.85f, 0.85f)
                drawMesh(torusRingMesh, floatArrayOf(0.2f, 1.0f, 0.4f, 1f), emissive = 0.65f)
                matrixStack.pop()
            }
            matrixStack.pop()

            // Ground Contact Shadow Decal
            matrixStack.push()
            matrixStack.translate(ball.position.x, 0.02f, ball.position.z)
            val shadowScale = ball.radius * 1.8f
            matrixStack.scale(shadowScale, 0.01f, shadowScale)
            drawMesh(cubeMesh, floatArrayOf(0.02f, 0.03f, 0.05f, 0.55f))
            matrixStack.pop()
        }
    }

    private fun renderPlayer() {
        val p = physics.player

        matrixStack.push()
        // Player position + natural body bobbing offset
        matrixStack.translate(p.position.x, p.position.y + p.bodyBobOffset, p.position.z)
        // Dynamic lateral banking tilt
        matrixStack.rotate(p.steerBankTilt, 0f, 0f, 1f)
        // Aerodynamic forward lean
        matrixStack.rotate(p.headPitch, 1f, 0f, 0f)

        // Ground shadow (strictly glued to road level)
        matrixStack.push()
        matrixStack.translate(0f, -p.position.y + 0.02f, 0f)
        val shadowAlpha = (1f - (p.position.y / 3.2f)).coerceIn(0.2f, 0.65f)
        matrixStack.scale(0.85f, 0.01f, 0.70f)
        drawMesh(cubeMesh, floatArrayOf(0.02f, 0.02f, 0.04f, shadowAlpha))
        matrixStack.pop()

        val legBaseY = p.shinLen + p.thighLen

        // 1. Pelvis / Waist (center of gravity)
        matrixStack.push()
        matrixStack.translate(0f, legBaseY, 0f)
        matrixStack.scale(0.44f, 0.16f, 0.28f)
        drawMesh(cubeMesh, p.suitSecondaryColor)
        matrixStack.pop()

        // 2. Athletic Torso with Armor Breastplate
        matrixStack.push()
        matrixStack.translate(0f, legBaseY + p.torsoHeight / 2f, 0f)
        // Torso counter-twist
        matrixStack.rotate(p.torsoTwist, 0f, 1f, 0f)

        // Main Torso Core
        matrixStack.push()
        matrixStack.scale(p.torsoWidth, p.torsoHeight, p.torsoDepth)
        drawMesh(cubeMesh, p.suitPrimaryColor)
        matrixStack.pop()

        // Front Chest Armor Plate
        matrixStack.push()
        matrixStack.translate(0f, 0.06f, -p.torsoDepth / 2f - 0.02f)
        matrixStack.scale(p.torsoWidth * 0.85f, p.torsoHeight * 0.70f, 0.06f)
        drawMesh(cubeMesh, p.armorPlateColor)
        matrixStack.pop()

        // Glowing Cyber Core Badge on chest
        matrixStack.push()
        matrixStack.translate(0f, 0.12f, -p.torsoDepth / 2f - 0.055f)
        matrixStack.scale(0.14f, 0.14f, 0.02f)
        drawMesh(cubeMesh, p.neonGlowColor, emissive = 1.0f)
        matrixStack.pop()

        // Cyber Thruster Pack on Back
        matrixStack.push()
        matrixStack.translate(0f, 0.05f, p.torsoDepth / 2f + 0.08f)
        matrixStack.scale(0.38f, 0.52f, 0.16f)
        drawMesh(cubeMesh, p.armorPlateColor)
        matrixStack.pop()

        // Thruster Exhaust Vents (Glowing)
        matrixStack.push()
        matrixStack.translate(-0.10f, -0.16f, p.torsoDepth / 2f + 0.15f)
        matrixStack.scale(0.08f, 0.08f, 0.05f)
        drawMesh(cubeMesh, p.neonGlowColor, emissive = 1.0f)
        matrixStack.pop()

        matrixStack.push()
        matrixStack.translate(0.10f, -0.16f, p.torsoDepth / 2f + 0.15f)
        matrixStack.scale(0.08f, 0.08f, 0.05f)
        drawMesh(cubeMesh, p.neonGlowColor, emissive = 1.0f)
        matrixStack.pop()

        matrixStack.pop() // End Torso

        // 3. Realistic Sculpted Head & Glowing Visor
        val headY = legBaseY + p.torsoHeight + p.headSize / 2f + 0.04f
        matrixStack.push()
        matrixStack.translate(0f, headY, 0f)

        // Head/Face Core
        matrixStack.push()
        matrixStack.scale(p.headSize * 0.85f, p.headSize, p.headSize * 0.88f)
        drawMesh(cubeMesh, p.skinColor)
        matrixStack.pop()

        // Aerodynamic Hair / Helmet
        matrixStack.push()
        matrixStack.translate(0f, p.headSize * 0.22f, p.headSize * 0.08f)
        matrixStack.scale(p.headSize * 0.95f, p.headSize * 0.55f, p.headSize * 0.95f)
        drawMesh(cubeMesh, p.hairColor)
        matrixStack.pop()

        // Reflective Glowing Cyber Visor
        matrixStack.push()
        matrixStack.translate(0f, 0.02f, -p.headSize * 0.44f - 0.02f)
        matrixStack.scale(p.headSize * 0.82f, 0.14f, 0.05f)
        drawMesh(cubeMesh, p.visorColor, emissive = 1.0f)
        matrixStack.pop()

        matrixStack.pop() // End Head

        // 4. Articulated Legs (Thigh + Shin + High-Top Sneakers)
        val hipOffsetX = 0.15f

        // LEFT LEG
        matrixStack.push()
        matrixStack.translate(-hipOffsetX, legBaseY, 0f)
        matrixStack.rotate(p.thighSwingLeft, 1f, 0f, 0f) // Hip stride rotation

        // Left Thigh
        matrixStack.push()
        matrixStack.translate(0f, -p.thighLen / 2f, 0f)
        matrixStack.scale(p.legThick, p.thighLen, p.legThick)
        drawMesh(cubeMesh, p.suitSecondaryColor)
        matrixStack.pop()

        // Left Knee Joint & Shin
        matrixStack.push()
        matrixStack.translate(0f, -p.thighLen, 0f)
        matrixStack.rotate(-p.kneeBendLeft, 1f, 0f, 0f) // Knee dynamic flexion

        // Knee Armor Cap
        matrixStack.push()
        matrixStack.translate(0f, 0f, -p.legThick * 0.55f)
        matrixStack.scale(p.legThick * 1.1f, p.legThick * 0.8f, 0.06f)
        drawMesh(cubeMesh, p.armorPlateColor)
        matrixStack.pop()

        // Shin / Calf
        matrixStack.push()
        matrixStack.translate(0f, -p.shinLen / 2f, 0f)
        matrixStack.scale(p.legThick * 0.92f, p.shinLen, p.legThick * 0.92f)
        drawMesh(cubeMesh, p.suitPrimaryColor)
        matrixStack.pop()

        // Left Athletic Running Sneaker
        matrixStack.push()
        matrixStack.translate(0f, -p.shinLen + 0.04f, -0.05f)
        matrixStack.scale(p.legThick * 1.05f, 0.10f, 0.32f)
        drawMesh(cubeMesh, p.suitSecondaryColor)
        matrixStack.pop()

        // Glowing Neon Sneaker Sole
        matrixStack.push()
        matrixStack.translate(0f, -p.shinLen, -0.05f)
        matrixStack.scale(p.legThick * 1.1f, 0.03f, 0.34f)
        drawMesh(cubeMesh, p.neonGlowColor, emissive = 1.0f)
        matrixStack.pop()

        matrixStack.pop() // End Left Shin
        matrixStack.pop() // End Left Leg

        // RIGHT LEG
        matrixStack.push()
        matrixStack.translate(hipOffsetX, legBaseY, 0f)
        matrixStack.rotate(p.thighSwingRight, 1f, 0f, 0f)

        // Right Thigh
        matrixStack.push()
        matrixStack.translate(0f, -p.thighLen / 2f, 0f)
        matrixStack.scale(p.legThick, p.thighLen, p.legThick)
        drawMesh(cubeMesh, p.suitSecondaryColor)
        matrixStack.pop()

        // Right Knee Joint & Shin
        matrixStack.push()
        matrixStack.translate(0f, -p.thighLen, 0f)
        matrixStack.rotate(-p.kneeBendRight, 1f, 0f, 0f)

        // Knee Armor Cap
        matrixStack.push()
        matrixStack.translate(0f, 0f, -p.legThick * 0.55f)
        matrixStack.scale(p.legThick * 1.1f, p.legThick * 0.8f, 0.06f)
        drawMesh(cubeMesh, p.armorPlateColor)
        matrixStack.pop()

        // Shin / Calf
        matrixStack.push()
        matrixStack.translate(0f, -p.shinLen / 2f, 0f)
        matrixStack.scale(p.legThick * 0.92f, p.shinLen, p.legThick * 0.92f)
        drawMesh(cubeMesh, p.suitPrimaryColor)
        matrixStack.pop()

        // Right Athletic Running Sneaker
        matrixStack.push()
        matrixStack.translate(0f, -p.shinLen + 0.04f, -0.05f)
        matrixStack.scale(p.legThick * 1.05f, 0.10f, 0.32f)
        drawMesh(cubeMesh, p.suitSecondaryColor)
        matrixStack.pop()

        // Glowing Neon Sneaker Sole
        matrixStack.push()
        matrixStack.translate(0f, -p.shinLen, -0.05f)
        matrixStack.scale(p.legThick * 1.1f, 0.03f, 0.34f)
        drawMesh(cubeMesh, p.neonGlowColor, emissive = 1.0f)
        matrixStack.pop()

        matrixStack.pop() // End Right Shin
        matrixStack.pop() // End Right Leg

        // 5. Articulated Arms (Bicep + Forearm in athletic running pose)
        val shoulderY = legBaseY + p.torsoHeight - 0.10f
        val shoulderX = p.torsoWidth / 2f + p.armThick / 2f + 0.03f

        // LEFT ARM (swings with right leg)
        matrixStack.push()
        matrixStack.translate(-shoulderX, shoulderY, 0f)
        matrixStack.rotate(p.armSwingLeft, 1f, 0f, 0f)

        // Shoulder Armor Pauldron
        matrixStack.push()
        matrixStack.translate(0f, 0.02f, 0f)
        matrixStack.scale(p.armThick * 1.3f, 0.12f, p.armThick * 1.3f)
        drawMesh(cubeMesh, p.armorPlateColor)
        matrixStack.pop()

        // Upper Arm (Bicep)
        matrixStack.push()
        matrixStack.translate(0f, -p.upperArmLen / 2f, 0f)
        matrixStack.scale(p.armThick, p.upperArmLen, p.armThick)
        drawMesh(cubeMesh, p.suitPrimaryColor)
        matrixStack.pop()

        // Forearm & Clenched Running Fist
        matrixStack.push()
        matrixStack.translate(0f, -p.upperArmLen, 0f)
        matrixStack.rotate(-p.elbowBendLeft, 1f, 0f, 0f) // Cocked in athletic runner bend

        // Forearm Gauntlet
        matrixStack.push()
        matrixStack.translate(0f, -p.foreArmLen / 2f, 0f)
        matrixStack.scale(p.armThick * 0.95f, p.foreArmLen, p.armThick * 0.95f)
        drawMesh(cubeMesh, p.suitSecondaryColor)
        matrixStack.pop()

        // Gauntlet neon strip
        matrixStack.push()
        matrixStack.translate(-p.armThick * 0.5f, -p.foreArmLen / 2f, 0f)
        matrixStack.scale(0.02f, p.foreArmLen * 0.7f, 0.05f)
        drawMesh(cubeMesh, p.neonGlowColor, emissive = 1.0f)
        matrixStack.pop()

        // Glove / Fist
        matrixStack.push()
        matrixStack.translate(0f, -p.foreArmLen, 0f)
        matrixStack.scale(p.armThick * 1.05f, 0.12f, p.armThick * 1.15f)
        drawMesh(cubeMesh, p.armorPlateColor)
        matrixStack.pop()

        matrixStack.pop() // End Left Forearm
        matrixStack.pop() // End Left Arm

        // RIGHT ARM (swings opposite)
        matrixStack.push()
        matrixStack.translate(shoulderX, shoulderY, 0f)
        matrixStack.rotate(p.armSwingRight, 1f, 0f, 0f)

        // Shoulder Armor Pauldron
        matrixStack.push()
        matrixStack.translate(0f, 0.02f, 0f)
        matrixStack.scale(p.armThick * 1.3f, 0.12f, p.armThick * 1.3f)
        drawMesh(cubeMesh, p.armorPlateColor)
        matrixStack.pop()

        // Upper Arm (Bicep)
        matrixStack.push()
        matrixStack.translate(0f, -p.upperArmLen / 2f, 0f)
        matrixStack.scale(p.armThick, p.upperArmLen, p.armThick)
        drawMesh(cubeMesh, p.suitPrimaryColor)
        matrixStack.pop()

        // Forearm & Clenched Running Fist
        matrixStack.push()
        matrixStack.translate(0f, -p.upperArmLen, 0f)
        matrixStack.rotate(-p.elbowBendRight, 1f, 0f, 0f)

        // Forearm Gauntlet
        matrixStack.push()
        matrixStack.translate(0f, -p.foreArmLen / 2f, 0f)
        matrixStack.scale(p.armThick * 0.95f, p.foreArmLen, p.armThick * 0.95f)
        drawMesh(cubeMesh, p.suitSecondaryColor)
        matrixStack.pop()

        // Gauntlet neon strip
        matrixStack.push()
        matrixStack.translate(p.armThick * 0.5f, -p.foreArmLen / 2f, 0f)
        matrixStack.scale(0.02f, p.foreArmLen * 0.7f, 0.05f)
        drawMesh(cubeMesh, p.neonGlowColor, emissive = 1.0f)
        matrixStack.pop()

        // Glove / Fist
        matrixStack.push()
        matrixStack.translate(0f, -p.foreArmLen, 0f)
        matrixStack.scale(p.armThick * 1.05f, 0.12f, p.armThick * 1.15f)
        drawMesh(cubeMesh, p.armorPlateColor)
        matrixStack.pop()

        matrixStack.pop() // End Right Forearm
        matrixStack.pop() // End Right Arm

        matrixStack.pop() // End Player
    }

    private fun renderParticles() {
        for (pt in physics.particles.particles) {
            if (pt.lifetime <= 0f) continue
            matrixStack.push()
            matrixStack.translate(pt.position.x, pt.position.y, pt.position.z)
            matrixStack.scale(pt.size, pt.size, pt.size)
            drawMesh(cubeMesh, pt.color, emissive = 0.85f)
            matrixStack.pop()
        }
    }
}

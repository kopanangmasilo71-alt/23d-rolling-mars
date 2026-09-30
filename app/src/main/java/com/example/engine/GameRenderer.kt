package com.example.engine

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sin

class GameRenderer(val physics: GamePhysicsEngine) : GLSurfaceView.Renderer {

    // Inputs managed from Compose UI
    @Volatile var isLeftHeld: Boolean = false
    @Volatile var isRightHeld: Boolean = false
    @Volatile var isBrakeHeld: Boolean = false
    @Volatile var isShootHeld: Boolean = false

    private var programId = 0
    private var uMVPMatrixLoc = 0
    private var uViewProjMatrixLoc = 0
    private var uModelMatrixLoc = 0
    private var uColorOverrideLoc = 0
    private var uCurvatureLoc = 0
    private var uCamPosLoc = 0
    private var uLightDirLoc = 0
    private var uLightColorLoc = 0
    private var uAmbientColorLoc = 0
    private var uFogColorLoc = 0
    private var uFogHorizonColorLoc = 0
    private var uFogStartLoc = 0
    private var uFogEndLoc = 0
    private var uEmissiveLoc = 0
    private var uTimeLoc = 0
    private var uSpeedRatioLoc = 0
    private var uHeadlightIntensityLoc = 0
    private var uPlayerPosLoc = 0

    private var aPositionLoc = 0
    private var aNormalLoc = 0
    private var aColorLoc = 0

    // Matrices
    private var viewportAspect: Float = 9f / 16f
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16).apply {
        Matrix.perspectiveM(this, 0, 64f, 9f / 16f, 0.5f, 160f)
    }
    private val projViewMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val matrixStack = MatrixStack()

    // Pre-allocated static arrays to guarantee zero GC allocation during 60 FPS rendering
    companion object {
        private val SIDE_OFFSETS = floatArrayOf(-1f, 1f)
        private val SIDE_AMMUNITION = floatArrayOf(-0.11f, 0.11f)
        private val WING_OFFSETS = floatArrayOf(-0.12f, 0.12f)
        private val FIN_ANGLES = floatArrayOf(-35f, -15f, 15f, 35f)
        private val PORT_OFFSETS_X = floatArrayOf(-0.16f, 0.16f, -0.16f, 0.16f)
        private val PORT_OFFSETS_Y = floatArrayOf(0.18f, 0.18f, -0.06f, -0.06f)
        private val CURB_RED = floatArrayOf(0.96f, 0.20f, 0.15f, 1f)
        private val CURB_WHITE = floatArrayOf(0.95f, 0.96f, 0.98f, 1f)
        private val ROAD_COLOR = floatArrayOf(0.11f, 0.13f, 0.18f, 1f)
        private val GRASS_COLOR = floatArrayOf(0.06f, 0.16f, 0.12f, 1f)
        private val NEON_CYAN = floatArrayOf(0.00f, 0.95f, 1.00f, 1.0f)
        private val WHITE_DIVIDER = floatArrayOf(0.88f, 0.90f, 0.96f, 0.70f)
        private val POST_COLOR = floatArrayOf(0.14f, 0.16f, 0.22f, 1f)
        private val CROSSBAR_COLOR = floatArrayOf(0.12f, 0.14f, 0.20f, 1f)
        private val SPEED_PAD_COLOR = floatArrayOf(0.0f, 0.95f, 1.0f, 1.0f)
        private val WHITE_COLOR = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
        private val SHADOW_COLOR = floatArrayOf(0.02f, 0.03f, 0.05f, 0.55f)
        private val TRAIL_OFFSETS = floatArrayOf(0.70f, 1.85f, 3.20f, 4.75f, 6.40f)
        private val TRAIL_BASE_LENS = floatArrayOf(1.10f, 1.40f, 1.60f, 1.80f, 2.00f)
        private val TRAIL_ALPHAS = floatArrayOf(0.75f, 0.55f, 0.35f, 0.20f, 0.10f)
    }

    // Meshes
    private var isReady: Boolean = false
    private var lastBoundMesh: Mesh? = null
    private lateinit var cubeMesh: Mesh
    private lateinit var roadQuadMesh: Mesh
    private lateinit var grassQuadMesh: Mesh
    private lateinit var cylinderMesh: Mesh
    private lateinit var smoothSphereMesh: Mesh
    private lateinit var torusRingMesh: Mesh
    private lateinit var skyBackdropMesh: Mesh
    private lateinit var starFieldMesh: Mesh
    private lateinit var speedPadMesh: Mesh
    private lateinit var crystalMesh: Mesh
    private lateinit var pyramidMesh: Mesh
    private lateinit var wingBladeMesh: Mesh
    private lateinit var wedgeMesh: Mesh
    private lateinit var magmaBoulderMesh: Mesh
    private lateinit var energyOrbMesh: Mesh
    private lateinit var coneMesh: Mesh

    // Ball Meshes
    private lateinit var straightBallMesh: Mesh
    private lateinit var crosserRightBallMesh: Mesh
    private lateinit var crosserLeftBallMesh: Mesh
    private lateinit var fastBallMesh: Mesh
    private lateinit var giantBallMesh: Mesh
    private lateinit var bouncerBallMesh: Mesh

    private var lastTimeNanos: Long = 0L
    private var totalTime: Float = 0f

    // Time-of-Day Lighting Engine & Atmosphere System
    val timeOfDaySystem = TimeOfDaySystem(cycleScoreLength = 10000)
    var currentTimeOfDay: TimeOfDaySnapshot = timeOfDaySystem.evaluate(0)
        private set

    private var currentFogR = 0.12f
    private var currentFogG = 0.09f
    private var currentFogB = 0.24f
    private var currentHorizR = 0.96f
    private var currentHorizG = 0.45f
    private var currentHorizB = 0.22f
    private var currentAmbR = 0.36f
    private var currentAmbG = 0.30f
    private var currentAmbB = 0.44f
    private var currentLightR = 1.0f
    private var currentLightG = 0.72f
    private var currentLightB = 0.52f
    private var currentLightDirX = 0.70f
    private var currentLightDirY = 0.35f
    private var currentLightDirZ = 0.45f

    private var currentSunPosX = 36f
    private var currentSunPosY = 12f
    private var currentSunPosZ = -85f
    private var currentSunR = 1.0f
    private var currentSunG = 0.65f
    private var currentSunB = 0.30f
    private var currentSunAlpha = 0.85f

    private var currentMoonPosX = -45f
    private var currentMoonPosY = 4f
    private var currentMoonPosZ = -85f
    private var currentMoonR = 0.75f
    private var currentMoonG = 0.85f
    private var currentMoonB = 1.0f
    private var currentMoonAlpha = 0.0f

    private var currentStarsAlpha = 0.30f
    private var currentHeadlightIntensity = 0.15f
    private var currentStreetLightEmissive = 0.40f

    private val vertexShaderSource = """
        precision mediump float;
        uniform mat4 uViewProjMatrix;
        uniform mat4 uModelMatrix;
        uniform vec4 uColorOverride;
        uniform vec3 uCamPos;
        uniform float uCurvature;
        attribute vec3 aPosition;
        attribute vec3 aNormal;
        attribute vec4 aColor;

        varying vec3 vNormal;
        varying vec3 vPosition;
        varying vec4 vColor;

        void main() {
            vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
            
            // Low-poly curved horizon world bending
            // Smoothly drop geometry over distance along the forward running axis (-Z)
            float forwardDist = max(0.0, uCamPos.z - worldPos.z);
            worldPos.y -= forwardDist * forwardDist * uCurvature;

            vPosition = worldPos.xyz;
            mat3 normalMatrix = mat3(uModelMatrix[0].xyz, uModelMatrix[1].xyz, uModelMatrix[2].xyz);
            vNormal = normalize(normalMatrix * aNormal);
            if (uColorOverride.a > 0.0) {
                vColor = uColorOverride;
            } else {
                vColor = aColor;
            }
            gl_Position = uViewProjMatrix * worldPos;
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
        uniform float uTime;
        uniform float uSpeedRatio;
        uniform float uHeadlightIntensity;
        uniform vec3 uPlayerPos;

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

            // Blinn-Phong Specular Highlight (safe base > 0.0)
            vec3 halfVector = normalize(lightDir + viewDir);
            float NdotH = max(dot(norm, halfVector), 0.0);
            float specFactor = (NdotH > 0.0) ? pow(NdotH, 32.0) : 0.0;
            vec3 specular = uLightColor * (specFactor * 0.50);

            // Fresnel Rim Lighting (safe base > 0.0)
            float NdotV = clamp(dot(norm, viewDir), 0.0, 1.0);
            float rimFactor = pow(1.0 - NdotV, 3.0);
            vec3 rimLight = vec3(0.35, 0.75, 1.0) * (rimFactor * 0.50);

            // Combined Surface Lighting
            vec3 surfaceLighting = uAmbientColor + diffuse + specular + rimLight;
            vec3 litColor = vColor.rgb * surfaceLighting;

            // Cyber Road Holographic Energy Grid Pulse
            if (vPosition.y < 0.08 && abs(vPosition.x) < 5.8) {
                float gridLine = step(0.90, fract(vPosition.z * 0.20 - uTime * 3.2 * uSpeedRatio));
                litColor += vec3(0.0, 0.55, 0.90) * gridLine * 0.35;
            }

            // Runner Forward Headlight Illumination Beam (Illuminates speedway ahead in evening and night)
            if (uHeadlightIntensity > 0.01 && vPosition.y < 0.65) {
                vec3 toFrag = vPosition - uPlayerPos;
                if (toFrag.z < 0.0 && toFrag.z > -32.0) {
                    float distZ = -toFrag.z;
                    float lateralDist = abs(toFrag.x - uPlayerPos.x);
                    float beamWidth = 2.4 + distZ * 0.26;
                    if (lateralDist < beamWidth) {
                        float spotAtten = (1.0 - lateralDist / beamWidth) * (1.0 - distZ / 32.0);
                        vec3 headlightBeam = vec3(0.60, 0.85, 1.0) * (spotAtten * uHeadlightIntensity * 0.65);
                        litColor += headlightBeam;
                    }
                }
            }

            // Emissive Glow Boost (for neon strips, visors, lamps, magma cores)
            if (uEmissive > 0.0) {
                litColor = mix(litColor, vColor.rgb * 1.6, uEmissive);
            }

            // Dual-Gradient Linear Fog & Horizon Depth Shading
            float dist = length(vPosition - uCamPos);
            float fogFactor = clamp((dist - uFogStart) / (uFogEnd - uFogStart), 0.0, 1.0);
            float smoothFog = smoothstep(0.0, 1.0, fogFactor);

            // Atmospheric Horizon Gradient: smooth vertical transition between sky and horizon
            float horizonBand = clamp((vPosition.y + 1.2) * 0.08, 0.0, 1.0);
            vec3 skyAtmosphere = mix(uFogColor, uFogHorizonColor, horizonBand);

            // Horizon subtle depth glow rim
            float depthGlow = smoothFog * (1.0 - smoothFog) * 0.20;
            skyAtmosphere += uFogHorizonColor * depthGlow;

            vec3 finalColor = mix(litColor, skyAtmosphere, smoothFog);

            gl_FragColor = vec4(finalColor, vColor.a);
        }
    """.trimIndent()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        try {
            isReady = false
            GLES20.glClearColor(0.06f, 0.08f, 0.16f, 1.0f)
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            GLES20.glDepthFunc(GLES20.GL_LEQUAL)
            GLES20.glDisable(GLES20.GL_CULL_FACE)

            programId = ShaderUtil.createProgram(vertexShaderSource, fragmentShaderSource)
            if (programId == 0) {
                android.util.Log.e("GameRenderer", "Failed to compile main shader, compiling resilient fallback shader")
                val fallbackVs = """
                    precision mediump float;
                    uniform mat4 uViewProjMatrix;
                    uniform mat4 uModelMatrix;
                    uniform vec4 uColorOverride;
                    attribute vec3 aPosition;
                    attribute vec4 aColor;
                    varying vec4 vColor;
                    void main() {
                        vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
                        vColor = (uColorOverride.a > 0.0) ? uColorOverride : aColor;
                        gl_Position = uViewProjMatrix * worldPos;
                    }
                """.trimIndent()
                val fallbackFs = """
                    precision mediump float;
                    varying vec4 vColor;
                    void main() {
                        gl_FragColor = vColor;
                    }
                """.trimIndent()
                programId = ShaderUtil.createProgram(fallbackVs, fallbackFs)
            }
            if (programId == 0) {
                android.util.Log.e("GameRenderer", "Fatal: both main and fallback shader failed")
                return
            }

            uViewProjMatrixLoc = GLES20.glGetUniformLocation(programId, "uViewProjMatrix")
            uCurvatureLoc = GLES20.glGetUniformLocation(programId, "uCurvature")
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
            uTimeLoc = GLES20.glGetUniformLocation(programId, "uTime")
            uSpeedRatioLoc = GLES20.glGetUniformLocation(programId, "uSpeedRatio")
            uHeadlightIntensityLoc = GLES20.glGetUniformLocation(programId, "uHeadlightIntensity")
            uPlayerPosLoc = GLES20.glGetUniformLocation(programId, "uPlayerPos")

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
            starFieldMesh = Primitives.createStarField(120, 95f)
            speedPadMesh = Primitives.createSpeedPad(3.0f, 4.2f)
            crystalMesh = Primitives.createFloatingCrystal(1.4f, 3.2f)
            pyramidMesh = Primitives.createSciFiPyramid(16f, 22f)
            wingBladeMesh = Primitives.createWingBlade(
                span = 0.90f,
                rootChord = 0.38f,
                tipChord = 0.12f,
                sweep = 0.24f,
                thickness = 0.035f,
                color = floatArrayOf(0.05f, 0.85f, 0.42f, 1f)
            )
            wedgeMesh = Primitives.createWedge(
                w = 0.25f,
                h = 0.45f,
                d = 0.35f,
                color = floatArrayOf(1f, 1f, 1f, 1f)
            )
            magmaBoulderMesh = Primitives.createMagmaBoulderMesh(radius = 1.0f)
            energyOrbMesh = Primitives.createEnergyOrbMesh(radius = 0.60f)
            coneMesh = Primitives.createTrafficCone(baseRadius = 0.32f, height = 0.72f, segments = 12)

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
            viewportAspect = width.toFloat() / h.toFloat()
            // Immersive dynamic perspective camera
            Matrix.perspectiveM(projectionMatrix, 0, 64f, viewportAspect, 0.5f, 160f)
        } catch (t: Throwable) {
            android.util.Log.e("GameRenderer", "Error during onSurfaceChanged", t)
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        try {
            val now = System.nanoTime()
            val dt = if (lastTimeNanos != 0L) ((now - lastTimeNanos) / 1_000_000_000.0f).coerceIn(0.001f, 0.05f) else 0.016f
            lastTimeNanos = now
            totalTime += dt

            // Update physics step
            physics.isShootHeld = isShootHeld
            physics.update(dt, isLeftHeld, isRightHeld, isBrakeHeld)

            // Evaluate dynamic Time-of-Day lighting & atmospheric system based on score
            val tod = timeOfDaySystem.evaluate(physics.score)
            currentTimeOfDay = tod
            val sec = physics.currentSector
            val lerpSpeed = (2.6f * dt).coerceAtMost(1f)

            // Modulate time of day with biome sector nuances
            val targetFogR = (tod.fogSkyColor[0] * 0.78f + sec.fogColor[0] * 0.22f).coerceIn(0f, 1f)
            val targetFogG = (tod.fogSkyColor[1] * 0.78f + sec.fogColor[1] * 0.22f).coerceIn(0f, 1f)
            val targetFogB = (tod.fogSkyColor[2] * 0.78f + sec.fogColor[2] * 0.22f).coerceIn(0f, 1f)

            val targetHorizR = (tod.fogHorizonColor[0] * 0.78f + sec.fogHorizonColor[0] * 0.22f).coerceIn(0f, 1f)
            val targetHorizG = (tod.fogHorizonColor[1] * 0.78f + sec.fogHorizonColor[1] * 0.22f).coerceIn(0f, 1f)
            val targetHorizB = (tod.fogHorizonColor[2] * 0.78f + sec.fogHorizonColor[2] * 0.22f).coerceIn(0f, 1f)

            val targetAmbR = (tod.ambientColor[0] * 0.82f + sec.ambientColor[0] * 0.18f).coerceIn(0f, 1f)
            val targetAmbG = (tod.ambientColor[1] * 0.82f + sec.ambientColor[1] * 0.18f).coerceIn(0f, 1f)
            val targetAmbB = (tod.ambientColor[2] * 0.82f + sec.ambientColor[2] * 0.18f).coerceIn(0f, 1f)

            val targetLightR = (tod.lightColor[0] * 0.85f + sec.lightColor[0] * 0.15f).coerceIn(0f, 1f)
            val targetLightG = (tod.lightColor[1] * 0.85f + sec.lightColor[1] * 0.15f).coerceIn(0f, 1f)
            val targetLightB = (tod.lightColor[2] * 0.85f + sec.lightColor[2] * 0.15f).coerceIn(0f, 1f)

            currentFogR += (targetFogR - currentFogR) * lerpSpeed
            currentFogG += (targetFogG - currentFogG) * lerpSpeed
            currentFogB += (targetFogB - currentFogB) * lerpSpeed

            currentHorizR += (targetHorizR - currentHorizR) * lerpSpeed
            currentHorizG += (targetHorizG - currentHorizG) * lerpSpeed
            currentHorizB += (targetHorizB - currentHorizB) * lerpSpeed

            currentAmbR += (targetAmbR - currentAmbR) * lerpSpeed
            currentAmbG += (targetAmbG - currentAmbG) * lerpSpeed
            currentAmbB += (targetAmbB - currentAmbB) * lerpSpeed

            currentLightR += (targetLightR - currentLightR) * lerpSpeed
            currentLightG += (targetLightG - currentLightG) * lerpSpeed
            currentLightB += (targetLightB - currentLightB) * lerpSpeed

            currentLightDirX += (tod.lightDir[0] - currentLightDirX) * lerpSpeed
            currentLightDirY += (tod.lightDir[1] - currentLightDirY) * lerpSpeed
            currentLightDirZ += (tod.lightDir[2] - currentLightDirZ) * lerpSpeed

            currentSunPosX += (tod.sunPos.x - currentSunPosX) * lerpSpeed
            currentSunPosY += (tod.sunPos.y - currentSunPosY) * lerpSpeed
            currentSunPosZ += (tod.sunPos.z - currentSunPosZ) * lerpSpeed
            currentSunR += (tod.sunColor[0] - currentSunR) * lerpSpeed
            currentSunG += (tod.sunColor[1] - currentSunG) * lerpSpeed
            currentSunB += (tod.sunColor[2] - currentSunB) * lerpSpeed
            currentSunAlpha += (tod.sunAlpha - currentSunAlpha) * lerpSpeed

            currentMoonPosX += (tod.moonPos.x - currentMoonPosX) * lerpSpeed
            currentMoonPosY += (tod.moonPos.y - currentMoonPosY) * lerpSpeed
            currentMoonPosZ += (tod.moonPos.z - currentMoonPosZ) * lerpSpeed
            currentMoonR += (tod.moonColor[0] - currentMoonR) * lerpSpeed
            currentMoonG += (tod.moonColor[1] - currentMoonG) * lerpSpeed
            currentMoonB += (tod.moonColor[2] - currentMoonB) * lerpSpeed
            currentMoonAlpha += (tod.moonAlpha - currentMoonAlpha) * lerpSpeed

            currentStarsAlpha += (tod.starsAlpha - currentStarsAlpha) * lerpSpeed
            currentHeadlightIntensity += (tod.headlightIntensity - currentHeadlightIntensity) * lerpSpeed
            currentStreetLightEmissive += (tod.streetLightEmissive - currentStreetLightEmissive) * lerpSpeed

            GLES20.glClearColor(currentFogR, currentFogG, currentFogB, 1.0f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            if (!isReady || programId == 0) return

            GLES20.glUseProgram(programId)

            // Dynamic FOV rush: expands perspective as player reaches hyper-speed and activates Overdrive
            val speedFovBonus = ((physics.player.forwardSpeed - 10f) * 0.75f).coerceIn(0f, 8f)
            val overdriveFovBonus = if (physics.player.isOverdriveActive) 7.5f else 0f
            val boostFovBonus = if (physics.player.isBoosting) 4.5f else 0f
            val dynamicFov = 64f + speedFovBonus + overdriveFovBonus + boostFovBonus
            Matrix.perspectiveM(projectionMatrix, 0, dynamicFov, viewportAspect, 0.5f, 160f)

            // Setup View Matrix with high-impact screen shake and dynamic camera banking
            val cam = physics.cameraPos
            val target = physics.cameraLookAt

            val shake = physics.cameraShakeMagnitude
            val shakeOffsetX = if (shake > 0.001f) (kotlin.random.Random.nextFloat() * 2f - 1f) * shake * 1.35f else 0f
            val shakeOffsetY = if (shake > 0.001f) (kotlin.random.Random.nextFloat() * 2f - 1f) * shake * 1.10f else 0f
            val shakeOffsetZ = if (shake > 0.001f) (kotlin.random.Random.nextFloat() * 2f - 1f) * shake * 0.55f else 0f

            // Dynamic camera banking: subtle, realistic roll into turns & cyber dash
            val bankAngle = (physics.player.steerBankTilt * 0.16f + (if (physics.player.isDashing) physics.player.dashDirection * 0.08f else 0f)).coerceIn(-0.07f, 0.07f)
            val upX = -kotlin.math.sin(bankAngle)
            val upY = kotlin.math.cos(bankAngle)

            Matrix.setLookAtM(
                viewMatrix, 0,
                cam.x + shakeOffsetX, cam.y + shakeOffsetY, cam.z + shakeOffsetZ,
                target.x + shakeOffsetX * 0.4f, target.y + shakeOffsetY * 0.4f, target.z,
                upX, upY, 0f
            )

            // Combine Projection and View matrices once per frame (avoids redundant calculations and buffer collision)
            Matrix.multiplyMM(projViewMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
            GLES20.glUniformMatrix4fv(uViewProjMatrixLoc, 1, false, projViewMatrix, 0)

            // Camera World Position for specular/rim lighting and curvature calculation
            GLES20.glUniform3f(uCamPosLoc, cam.x, cam.y, cam.z)

            // Directional Sun/Moon Celestial Light + Ambient
            GLES20.glUniform3f(uLightDirLoc, currentLightDirX, currentLightDirY, currentLightDirZ)
            GLES20.glUniform3f(uLightColorLoc, currentLightR, currentLightG, currentLightB)
            GLES20.glUniform3f(uAmbientColorLoc, currentAmbR, currentAmbG, currentAmbB)

            // Dual-Gradient Linear Fog & Horizon Depth Shading (smooth low-poly distance blend)
            GLES20.glUniform3f(uFogColorLoc, currentFogR, currentFogG, currentFogB)
            GLES20.glUniform3f(uFogHorizonColorLoc, currentHorizR, currentHorizG, currentHorizB)
            GLES20.glUniform1f(uFogStartLoc, 24.0f)
            GLES20.glUniform1f(uFogEndLoc, 120.0f)

            // Dynamic Forward Runner Headlight cone & Player position
            GLES20.glUniform1f(uHeadlightIntensityLoc, currentHeadlightIntensity)
            GLES20.glUniform3f(uPlayerPosLoc, physics.player.position.x, physics.player.position.y, physics.player.position.z)

            // Shader Animation Uniforms
            GLES20.glUniform1f(uTimeLoc, totalTime)
            val speedRatio = if (physics.isRunning) {
                (physics.player.forwardSpeed / physics.player.baseNormalSpeed) * sec.speedMultiplier
            } else 0f
            GLES20.glUniform1f(uSpeedRatioLoc, speedRatio)

            matrixStack.reset()
            lastBoundMesh = null

            // 1. Panoramic Sky Backdrop & Celestial Horizon Sun (rendered with depth write disabled)
            renderSkyBackdrop()

            // 2. High-Tech Speedway & Overhead Cyber Gates
            renderEnvironment()

            // 3. Glowing Sci-Fi Speed Pads
            renderSpeedPads()

            // 3b. Floating Collectible Power-Ups (Shields, Hyper Boosts, 2X Multipliers, Energy Cores)
            renderCollectibles()

            // 3c. Road Deflection Traffic Cones (Unpredictable bouncing obstacles)
            renderRoadCones()

            // 4. Roadside Scenery (Cyber Pylons, Trees, Crystals, Pyramids, Rocks)
            renderScenery()

            // 5. Rolling Boulders with Emissive Energy Halos
            renderBalls()

            // 5b. Flying Plasma Projectiles & Laser Blasts
            renderProjectiles()

            // 6. Realistic Articulated Hero Runner
            renderPlayer()

            // 7. Particle Energy, Dust, Sparks & Speed Streaks
            renderParticles()
        } catch (t: Throwable) {
            android.util.Log.e("GameRenderer", "Error in onDrawFrame", t)
        }
    }

    private fun drawMesh(
        mesh: Mesh,
        r: Float,
        g: Float,
        b: Float,
        a: Float = 1f,
        emissive: Float = 0f,
        curvature: Float = 0.00095f
    ) {
        if (!isReady || programId == 0 || aPositionLoc < 0) return

        val model = matrixStack.get()
        if (uModelMatrixLoc >= 0) GLES20.glUniformMatrix4fv(uModelMatrixLoc, 1, false, model, 0)
        if (uCurvatureLoc >= 0) GLES20.glUniform1f(uCurvatureLoc, curvature)
        if (uEmissiveLoc >= 0) GLES20.glUniform1f(uEmissiveLoc, emissive)
        if (uColorOverrideLoc >= 0) GLES20.glUniform4f(uColorOverrideLoc, r, g, b, a)

        if (lastBoundMesh !== mesh) {
            lastBoundMesh = mesh
            mesh.vertexBuffer.position(0)
            GLES20.glVertexAttribPointer(aPositionLoc, Primitives.POSITION_COMPONENT_COUNT, GLES20.GL_FLOAT, false, Primitives.STRIDE, mesh.vertexBuffer)
            GLES20.glEnableVertexAttribArray(aPositionLoc)

            if (aNormalLoc >= 0) {
                mesh.vertexBuffer.position(Primitives.POSITION_COMPONENT_COUNT)
                GLES20.glVertexAttribPointer(aNormalLoc, Primitives.NORMAL_COMPONENT_COUNT, GLES20.GL_FLOAT, false, Primitives.STRIDE, mesh.vertexBuffer)
                GLES20.glEnableVertexAttribArray(aNormalLoc)
            }

            if (aColorLoc >= 0) {
                mesh.vertexBuffer.position(Primitives.POSITION_COMPONENT_COUNT + Primitives.NORMAL_COMPONENT_COUNT)
                GLES20.glVertexAttribPointer(aColorLoc, Primitives.COLOR_COMPONENT_COUNT, GLES20.GL_FLOAT, false, Primitives.STRIDE, mesh.vertexBuffer)
                GLES20.glEnableVertexAttribArray(aColorLoc)
            }
        }

        mesh.indexBuffer.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mesh.indexCount, GLES20.GL_UNSIGNED_SHORT, mesh.indexBuffer)
    }

    private fun drawMesh(
        mesh: Mesh,
        colorOverride: FloatArray? = null,
        emissive: Float = 0f,
        curvature: Float = 0.00095f
    ) {
        if (!isReady || programId == 0 || aPositionLoc < 0) return

        val model = matrixStack.get()
        if (uModelMatrixLoc >= 0) GLES20.glUniformMatrix4fv(uModelMatrixLoc, 1, false, model, 0)
        if (uCurvatureLoc >= 0) GLES20.glUniform1f(uCurvatureLoc, curvature)
        if (uEmissiveLoc >= 0) GLES20.glUniform1f(uEmissiveLoc, emissive)

        if (uColorOverrideLoc >= 0) {
            if (colorOverride != null && colorOverride.isNotEmpty()) {
                val r = colorOverride[0]
                val g = if (colorOverride.size > 1) colorOverride[1] else 1f
                val b = if (colorOverride.size > 2) colorOverride[2] else 1f
                val a = if (colorOverride.size > 3) colorOverride[3] else 1f
                GLES20.glUniform4f(uColorOverrideLoc, r, g, b, a)
            } else {
                GLES20.glUniform4f(uColorOverrideLoc, 0f, 0f, 0f, 0f)
            }
        }

        if (lastBoundMesh !== mesh) {
            lastBoundMesh = mesh
            mesh.vertexBuffer.position(0)
            GLES20.glVertexAttribPointer(aPositionLoc, Primitives.POSITION_COMPONENT_COUNT, GLES20.GL_FLOAT, false, Primitives.STRIDE, mesh.vertexBuffer)
            GLES20.glEnableVertexAttribArray(aPositionLoc)

            if (aNormalLoc >= 0) {
                mesh.vertexBuffer.position(Primitives.POSITION_COMPONENT_COUNT)
                GLES20.glVertexAttribPointer(aNormalLoc, Primitives.NORMAL_COMPONENT_COUNT, GLES20.GL_FLOAT, false, Primitives.STRIDE, mesh.vertexBuffer)
                GLES20.glEnableVertexAttribArray(aNormalLoc)
            }

            if (aColorLoc >= 0) {
                mesh.vertexBuffer.position(Primitives.POSITION_COMPONENT_COUNT + Primitives.NORMAL_COMPONENT_COUNT)
                GLES20.glVertexAttribPointer(aColorLoc, Primitives.COLOR_COMPONENT_COUNT, GLES20.GL_FLOAT, false, Primitives.STRIDE, mesh.vertexBuffer)
                GLES20.glEnableVertexAttribArray(aColorLoc)
            }
        }

        mesh.indexBuffer.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mesh.indexCount, GLES20.GL_UNSIGNED_SHORT, mesh.indexBuffer)
    }

    private fun renderSkyBackdrop() {
        val cam = physics.cameraPos

        // Disable depth writing so the sky backdrop stays strictly behind all 3D scene elements
        GLES20.glDepthMask(false)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // 1. Panoramic Horizon Mountains & Atmospheric Sky Dome
        matrixStack.push()
        matrixStack.translate(cam.x, -5f, cam.z)
        drawMesh(skyBackdropMesh, emissive = 0.30f + currentStreetLightEmissive * 0.15f, curvature = 0f)
        matrixStack.pop()

        // 2. Twinkling Celestial Starfield Canopy (emerges at dusk, brilliant at midnight)
        if (currentStarsAlpha > 0.02f) {
            matrixStack.push()
            matrixStack.translate(cam.x, 10f, cam.z)
            val starTwinkle = 0.82f + 0.18f * sin(totalTime * 3.8f)
            val starAlpha = (currentStarsAlpha * starTwinkle).coerceIn(0f, 1f)
            drawMesh(
                starFieldMesh,
                colorOverride = floatArrayOf(0.92f, 0.96f, 1.0f, starAlpha),
                emissive = 0.95f,
                curvature = 0f
            )
            matrixStack.pop()
        }

        // 3. Radiant Celestial Sun (Rises in east at dawn, arcs across sky, sets in west at sunset)
        if (currentSunAlpha > 0.02f) {
            matrixStack.push()
            matrixStack.translate(cam.x + currentSunPosX, currentSunPosY, cam.z + currentSunPosZ)
            matrixStack.scale(12f, 12f, 0.1f)
            val sunCol = floatArrayOf(currentSunR, currentSunG, currentSunB, currentSunAlpha)
            drawMesh(smoothSphereMesh, colorOverride = sunCol, emissive = 0.95f, curvature = 0f)

            // Outer radiant corona ring
            matrixStack.scale(1.35f, 1.35f, 1.0f)
            val coronaCol = floatArrayOf(currentSunR, currentSunG * 0.9f, currentSunB * 0.7f, currentSunAlpha * 0.48f)
            drawMesh(torusRingMesh, colorOverride = coronaCol, emissive = 0.80f, curvature = 0f)
            matrixStack.pop()
        }

        // 4. Luminous Celestial Moon (Rises at dusk, shines through midnight)
        if (currentMoonAlpha > 0.02f) {
            matrixStack.push()
            matrixStack.translate(cam.x + currentMoonPosX, currentMoonPosY, cam.z + currentMoonPosZ)
            matrixStack.scale(9.5f, 9.5f, 0.1f)
            val moonCol = floatArrayOf(currentMoonR, currentMoonG, currentMoonB, currentMoonAlpha)
            drawMesh(smoothSphereMesh, colorOverride = moonCol, emissive = 0.90f, curvature = 0f)

            // Ethereal lunar halo ring
            matrixStack.scale(1.30f, 1.30f, 1.0f)
            val haloCol = floatArrayOf(0.60f, 0.80f, 1.00f, currentMoonAlpha * 0.40f)
            drawMesh(torusRingMesh, colorOverride = haloCol, emissive = 0.75f, curvature = 0f)
            matrixStack.pop()
        }

        // Re-enable depth write for road, player, obstacles, and scenery
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDepthMask(true)
    }

    private fun renderEnvironment() {
        val halfW = physics.roadHalfWidth
        val segLen = physics.segmentLength

        for (seg in physics.roadSegments) {
            val centerZ = seg.zStart - segLen / 2f

            // Asphalt Speedway Surface
            matrixStack.push()
            matrixStack.translate(0f, 0f, centerZ)
            drawMesh(roadQuadMesh, ROAD_COLOR)
            matrixStack.pop()

            // Left & Right Terrain Banks
            matrixStack.push()
            matrixStack.translate(-(halfW + 22.5f), -0.02f, centerZ)
            drawMesh(grassQuadMesh, GRASS_COLOR)
            matrixStack.pop()

            matrixStack.push()
            matrixStack.translate(halfW + 22.5f, -0.02f, centerZ)
            drawMesh(grassQuadMesh, GRASS_COLOR)
            matrixStack.pop()

            // Curbs: Alternating Red & White Hazard Curbs (Matches Reference Art!)
            val curbBlocks = 6
            val curbStep = segLen / curbBlocks
            for (cb in 0 until curbBlocks) {
                val cz = seg.zStart - (cb + 0.5f) * curbStep
                val isRed = (seg.id * curbBlocks + cb) % 2 == 0
                val curbCol = if (isRed) CURB_RED else CURB_WHITE

                // Left curb
                matrixStack.push()
                matrixStack.translate(-halfW, 0.16f, cz)
                matrixStack.scale(0.38f, 0.32f, curbStep * 0.98f)
                drawMesh(cubeMesh, curbCol)
                matrixStack.pop()

                // Right curb
                matrixStack.push()
                matrixStack.translate(halfW, 0.16f, cz)
                matrixStack.scale(0.38f, 0.32f, curbStep * 0.98f)
                drawMesh(cubeMesh, curbCol)
                matrixStack.pop()
            }

            // Glowing Center Dashed Lines (Intense Neon Cyan) & Outer Lane Dividers
            val dashes = 6
            val dashStep = segLen / dashes
            for (d in 0 until dashes) {
                val dz = seg.zStart - (d + 0.5f) * dashStep

                // Luminous Neon Cyan Center Line Strip
                matrixStack.push()
                matrixStack.translate(0f, 0.026f, dz)
                matrixStack.scale(0.32f, 0.022f, 3.2f)
                drawMesh(cubeMesh, NEON_CYAN, emissive = 1.0f)
                matrixStack.pop()

                // Left White Lane Divider Marking
                matrixStack.push()
                matrixStack.translate(-2.4f, 0.02f, dz)
                matrixStack.scale(0.14f, 0.018f, 1.8f)
                drawMesh(cubeMesh, WHITE_DIVIDER)
                matrixStack.pop()

                // Right White Lane Divider Marking
                matrixStack.push()
                matrixStack.translate(2.4f, 0.02f, dz)
                matrixStack.scale(0.14f, 0.018f, 1.8f)
                drawMesh(cubeMesh, WHITE_DIVIDER)
                matrixStack.pop()
            }

            // Overhead Sci-Fi Portal Gateway Frames spanning the track (every 2 segments = 60m)
            if (seg.id % 2 == 0) {
                matrixStack.push()
                matrixStack.translate(0f, 0f, seg.zStart)

                // Left Dark Post
                matrixStack.push()
                matrixStack.translate(-halfW - 0.35f, 3.6f, 0f)
                matrixStack.scale(0.40f, 7.2f, 0.40f)
                drawMesh(cubeMesh, POST_COLOR)
                matrixStack.pop()

                // Right Dark Post
                matrixStack.push()
                matrixStack.translate(halfW + 0.35f, 3.6f, 0f)
                matrixStack.scale(0.40f, 7.2f, 0.40f)
                drawMesh(cubeMesh, POST_COLOR)
                matrixStack.pop()

                // Overhead Dark Crossbar
                matrixStack.push()
                matrixStack.translate(0f, 7.2f, 0f)
                matrixStack.scale(physics.roadWidth + 1.1f, 0.55f, 0.45f)
                drawMesh(cubeMesh, CROSSBAR_COLOR)
                matrixStack.pop()

                // Glowing Neon Cyan Portal Frame Strips (Facing Player)
                // Left inner neon strip
                matrixStack.push()
                matrixStack.translate(-halfW - 0.12f, 3.6f, -0.22f)
                matrixStack.scale(0.08f, 7.0f, 0.04f)
                drawMesh(cubeMesh, NEON_CYAN, emissive = 1.0f)
                matrixStack.pop()

                // Right inner neon strip
                matrixStack.push()
                matrixStack.translate(halfW + 0.12f, 3.6f, -0.22f)
                matrixStack.scale(0.08f, 7.0f, 0.04f)
                drawMesh(cubeMesh, NEON_CYAN, emissive = 1.0f)
                matrixStack.pop()

                // Top crossbar neon strip
                matrixStack.push()
                matrixStack.translate(0f, 7.15f, -0.24f)
                matrixStack.scale(physics.roadWidth + 0.6f, 0.12f, 0.04f)
                drawMesh(cubeMesh, NEON_CYAN, emissive = 1.0f)
                matrixStack.pop()

                matrixStack.pop()
            }
        }
    }

    private fun renderSpeedPads() {
        val camZ = physics.cameraPos.z
        for (pad in physics.speedPadPool) {
            if (!pad.isActive || pad.position.z > camZ + 8f || pad.position.z < camZ - 95f) continue
            matrixStack.push()
            matrixStack.translate(pad.position.x, pad.position.y, pad.position.z)
            val glow = 0.70f + sin(pad.glowPulse) * 0.30f
            drawMesh(speedPadMesh, floatArrayOf(0.0f, 0.95f, 1.0f, 1.0f), emissive = glow)

            // Speed Chevron Accent
            matrixStack.push()
            matrixStack.translate(0f, 0.04f, 0f)
            matrixStack.scale(0.8f, 0.02f, 0.8f)
            drawMesh(cubeMesh, floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f), emissive = 1.0f)
            matrixStack.pop()

            matrixStack.pop()
        }
    }

    private fun renderCollectibles() {
        val camZ = physics.cameraPos.z
        for (col in physics.collectiblePool) {
            if (!col.isActive || col.position.z > camZ + 8f || col.position.z < camZ - 95f) continue
            matrixStack.push()
            matrixStack.translate(col.position.x, col.position.y, col.position.z)
            matrixStack.rotate(col.rotationY, 0f, 1f, 0f)

            // Render 3D icon and effects based on collectible type
            when (col.type) {
                CollectibleType.SHIELD -> {
                    // Shimmering Hexagonal Diamond Crystal with Orbiting Ring
                    matrixStack.push()
                    matrixStack.scale(0.55f, 0.70f, 0.55f)
                    drawMesh(crystalMesh, floatArrayOf(0.0f, 0.95f, 1.0f, 1.0f), emissive = 0.90f)
                    matrixStack.pop()

                    // Orbiting Shield Ring
                    matrixStack.push()
                    matrixStack.rotate(totalTime * 110f, 1f, 0.5f, 0f)
                    matrixStack.scale(0.80f, 0.80f, 0.80f)
                    drawMesh(torusRingMesh, floatArrayOf(0.3f, 1.0f, 1.0f, 1.0f), emissive = 1.0f)
                    matrixStack.pop()
                }
                CollectibleType.SPEED_BOOST -> {
                    // Emerald Turbo Crystal
                    matrixStack.push()
                    matrixStack.rotate(45f, 0f, 0f, 1f)
                    matrixStack.scale(0.52f, 0.52f, 0.52f)
                    drawMesh(cubeMesh, floatArrayOf(0.0f, 1.0f, 0.55f, 1.0f), emissive = 1.0f)
                    matrixStack.pop()

                    matrixStack.push()
                    matrixStack.scale(0.70f, 0.70f, 0.70f)
                    drawMesh(torusRingMesh, floatArrayOf(0.2f, 1.0f, 0.6f, 1.0f), emissive = 0.85f)
                    matrixStack.pop()
                }
                CollectibleType.SCORE_MULTIPLIER -> {
                    // Radiant Golden Star Gem with Gold Halo
                    matrixStack.push()
                    matrixStack.rotate(totalTime * 75f, 1f, 1f, 0f)
                    matrixStack.scale(0.52f, 0.52f, 0.52f)
                    drawMesh(cubeMesh, floatArrayOf(1.0f, 0.85f, 0.12f, 1.0f), emissive = 1.0f)
                    matrixStack.pop()

                    matrixStack.push()
                    matrixStack.scale(0.72f, 0.72f, 0.72f)
                    drawMesh(torusRingMesh, floatArrayOf(1.0f, 0.95f, 0.4f, 1.0f), emissive = 0.95f)
                    matrixStack.pop()
                }
                CollectibleType.ENERGY_CELL -> {
                    // Glowing Magenta Credit Core
                    matrixStack.push()
                    matrixStack.scale(0.42f, 0.42f, 0.42f)
                    drawMesh(smoothSphereMesh, floatArrayOf(1.0f, 0.25f, 0.85f, 1.0f), emissive = 1.0f)
                    matrixStack.pop()
                }
                CollectibleType.ENERGY_ORB -> {
                    // Golden Energy Orb with sparkling rotating halo (Matches design art!)
                    matrixStack.push()
                    matrixStack.scale(0.85f, 0.85f, 0.85f)
                    drawMesh(energyOrbMesh, floatArrayOf(1.0f, 0.88f, 0.15f, 1.0f), emissive = 1.0f)
                    matrixStack.pop()

                    // Orbiting golden halo ring
                    matrixStack.push()
                    matrixStack.rotate(totalTime * 120f, 0.5f, 1f, 0.2f)
                    matrixStack.scale(0.85f, 0.85f, 0.85f)
                    drawMesh(torusRingMesh, floatArrayOf(1.0f, 0.95f, 0.35f, 1.0f), emissive = 1.0f)
                    matrixStack.pop()
                }
                CollectibleType.AMMO_PACK -> {
                    // Glowing Plasma Ammo Crate Battery
                    matrixStack.push()
                    matrixStack.rotate(totalTime * 80f, 0f, 1f, 0f)
                    matrixStack.scale(0.38f, 0.46f, 0.26f)
                    drawMesh(cubeMesh, floatArrayOf(0.18f, 0.16f, 0.20f, 1f))
                    matrixStack.pop()

                    // Dual glowing plasma cartridges
                    for (side in SIDE_AMMUNITION) {
                        matrixStack.push()
                        matrixStack.rotate(totalTime * 80f, 0f, 1f, 0f)
                        matrixStack.translate(side, 0f, 0f)
                        matrixStack.scale(0.08f, 0.38f, 0.12f)
                        drawMesh(cubeMesh, 1.0f, 0.45f, 0.05f, 1f, emissive = 1.0f)
                        matrixStack.pop()
                    }

                    // Rotating fiery energy halo ring
                    matrixStack.push()
                    matrixStack.rotate(totalTime * 130f, 1f, 0.5f, 0f)
                    matrixStack.scale(0.68f, 0.68f, 0.68f)
                    drawMesh(torusRingMesh, floatArrayOf(1.0f, 0.55f, 0.10f, 1f), emissive = 0.95f)
                    matrixStack.pop()
                }
            }

            matrixStack.pop()

            // Ground Shadow Decal under floating collectible
            matrixStack.push()
            matrixStack.translate(col.position.x, 0.02f, col.position.z)
            val shadowPulse = (0.75f - (col.position.y - 0.85f) * 0.4f).coerceIn(0.4f, 0.9f)
            matrixStack.scale(shadowPulse, 0.01f, shadowPulse)
            drawMesh(cubeMesh, floatArrayOf(0.02f, 0.03f, 0.06f, 0.50f))
            matrixStack.pop()
        }
    }

    private fun renderRoadCones() {
        val camZ = physics.cameraPos.z
        for (cone in physics.conePool) {
            if (!cone.isActive || cone.position.z > camZ + 12f || cone.position.z < camZ - 105f) continue
            matrixStack.push()
            matrixStack.translate(cone.position.x, cone.position.y, cone.position.z)

            // Dynamic elastic rubber wobble or tumbling airborne rotations
            if (cone.wobbleAngleX != 0f || cone.wobbleAngleZ != 0f) {
                matrixStack.rotate(cone.wobbleAngleX, 1f, 0f, 0f)
                matrixStack.rotate(cone.wobbleAngleZ, 0f, 0f, 1f)
            }

            drawMesh(coneMesh)

            // Soft shadow under grounded cone
            if (!cone.isLaunched && cone.position.y <= 0.05f) {
                matrixStack.push()
                matrixStack.translate(0f, 0.015f, 0f)
                matrixStack.scale(0.55f, 0.01f, 0.55f)
                drawMesh(cubeMesh, SHADOW_COLOR)
                matrixStack.pop()
            }

            matrixStack.pop()
        }
    }

    private fun renderScenery() {
        val camZ = physics.cameraPos.z
        for (item in physics.scenery) {
            // Frustum Culling Optimization: Skip objects far behind camera or beyond fog horizon
            if (item.z > camZ + 12f || item.z < camZ - 105f) continue

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

                    matrixStack.push()
                    matrixStack.translate(if (item.x < 0) 0.6f else -0.6f, 6.0f, 0f)
                    matrixStack.scale(1.2f, 0.25f, 0.5f)
                    drawMesh(cubeMesh, floatArrayOf(0.25f, 0.28f, 0.38f, 1f))
                    matrixStack.pop()

                    // Glowing LED lens with time-of-day dynamic street lighting
                    matrixStack.push()
                    matrixStack.translate(if (item.x < 0) 0.6f else -0.6f, 5.85f, 0f)
                    matrixStack.scale(1.0f, 0.08f, 0.4f)
                    val lampColor = if (currentStreetLightEmissive > 0.5f) {
                        floatArrayOf(1.0f, 0.85f, 0.35f, 1f) // Golden warm street beacon at night
                    } else {
                        floatArrayOf(0.00f, 0.95f, 1.00f, 1f) // Cool cyan standby LED during day
                    }
                    val lampEmissive = 0.4f + currentStreetLightEmissive * 0.6f
                    drawMesh(cubeMesh, lampColor, emissive = lampEmissive)
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
                3 -> {
                    // Floating Sci-Fi Crystal Monolith
                    val bob = sin(totalTime * 2.2f + item.x) * 0.45f + 2.8f
                    matrixStack.push()
                    matrixStack.translate(0f, bob, 0f)
                    matrixStack.rotate(totalTime * 35f, 0f, 1f, 0f)
                    drawMesh(crystalMesh, emissive = 0.75f)
                    matrixStack.pop()
                }
                4 -> {
                    // Monumental Futuristic Pyramid
                    matrixStack.push()
                    drawMesh(pyramidMesh, emissive = 0.15f)

                    // Glowing apex beacon
                    matrixStack.push()
                    matrixStack.translate(0f, 22.2f, 0f)
                    matrixStack.scale(1.2f, 1.2f, 1.2f)
                    val horiz = physics.currentSector.fogHorizonColor
                    drawMesh(cubeMesh, floatArrayOf(horiz[0], horiz[1], horiz[2], 1.0f), emissive = 1.0f)
                    matrixStack.pop()

                    matrixStack.pop()
                }
            }
            matrixStack.pop()
        }
    }

    private fun renderBalls() {
        val camZ = physics.cameraPos.z
        val threatGlowBoost = (physics.dynamicSpeedMultiplier - 1.0f) * 0.30f
        for (ball in physics.ballPool) {
            if (!ball.isActive || ball.position.z > camZ + 12f || ball.position.z < camZ - 110f) continue

            matrixStack.push()
            matrixStack.translate(ball.position.x, ball.position.y, ball.position.z)
            // Roll rotation along forward and horizontal axes
            matrixStack.rotate(ball.rollAngleX, 1f, 0f, 0f)
            matrixStack.rotate(ball.rollAngleZ, 0f, 0f, 1f)
            // Personality wobble effect (Matches "Obstacles have personality: Rotate & wobble"!)
            matrixStack.rotate(sin(ball.rollAngleX * 0.06f) * 7f, 0f, 0f, 1f)

            when (ball.ballType) {
                BallType.STRAIGHT, BallType.FAST, BallType.GIANT -> {
                    // Volcanic Magma Boulder with dark basalt rock facets & molten orange/gold crevices
                    matrixStack.push()
                    matrixStack.scale(ball.radius, ball.radius, ball.radius)
                    val baseEmissive = if (ball.ballType == BallType.FAST) 0.85f else 0.65f
                    val magmaEmissive = (baseEmissive + threatGlowBoost).coerceAtMost(1.0f)
                    drawMesh(magmaBoulderMesh, emissive = magmaEmissive)
                    matrixStack.pop()
                }
                BallType.LEFT_TO_RIGHT -> {
                    drawMesh(crosserRightBallMesh, emissive = (0.25f + threatGlowBoost).coerceAtMost(0.85f))
                }
                BallType.RIGHT_TO_LEFT -> {
                    drawMesh(crosserLeftBallMesh, emissive = (0.25f + threatGlowBoost).coerceAtMost(0.85f))
                }
                BallType.BOUNCING -> {
                    drawMesh(bouncerBallMesh, emissive = (0.35f + threatGlowBoost).coerceAtMost(0.90f))
                }
            }

            // Orbiting glowing energy halo rings
            if (ball.ballType == BallType.GIANT) {
                matrixStack.push()
                matrixStack.rotate(totalTime * 60f, 0f, 1f, 0f)
                matrixStack.scale(1.20f, 1.20f, 1.20f)
                drawMesh(torusRingMesh, floatArrayOf(1.0f, 0.45f, 0.10f, 1f), emissive = 0.85f)
                matrixStack.pop()
            } else if (ball.ballType == BallType.BOUNCING) {
                matrixStack.push()
                matrixStack.scale(0.88f, 0.88f, 0.88f)
                drawMesh(torusRingMesh, floatArrayOf(0.2f, 1.0f, 0.4f, 1f), emissive = 0.70f)
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

        if (p.isTumbling) {
            matrixStack.rotate(p.tumblePitch, 1f, 0f, 0f)
            matrixStack.rotate(p.tumbleRoll, 0f, 0f, 1f)
            matrixStack.rotate(p.tumbleYaw, 0f, 1f, 0f)
        } else {
            // Dynamic body yaw into lateral direction
            matrixStack.rotate(p.bodyYaw, 0f, 1f, 0f)
            // Dynamic lateral banking tilt
            matrixStack.rotate(p.steerBankTilt, 0f, 0f, 1f)
            // Aerodynamic forward lean
            matrixStack.rotate(p.headPitch, 1f, 0f, 0f)
        }

        // Ground shadow (strictly glued to road level)
        matrixStack.push()
        matrixStack.translate(0f, -p.position.y + 0.02f, 0f)
        val shadowAlpha = (1f - (p.position.y / 3.2f)).coerceIn(0.2f, 0.65f)
        val shadowW = if (p.model == CharacterModelId.TITAN) 1.15f else 0.85f
        matrixStack.scale(shadowW, 0.01f, 0.70f)
        drawMesh(cubeMesh, floatArrayOf(0.02f, 0.02f, 0.04f, shadowAlpha))
        matrixStack.pop()

        val legBaseY = p.shinLen + p.thighLen

        // 1. Pelvis / Waist (center of gravity)
        matrixStack.push()
        matrixStack.translate(0f, legBaseY, 0f)
        val waistW = p.torsoWidth * 0.76f
        matrixStack.scale(waistW, 0.16f, p.torsoDepth * 0.85f)
        drawMesh(cubeMesh, p.suitSecondaryColor)
        matrixStack.pop()

        // TITAN: Armored Hip Skirt Guards
        if (p.model == CharacterModelId.TITAN) {
            for (side in SIDE_OFFSETS) {
                matrixStack.push()
                matrixStack.translate(side * (waistW / 2f + 0.04f), legBaseY - 0.04f, 0f)
                matrixStack.scale(0.08f, 0.20f, p.torsoDepth * 0.90f)
                drawMesh(cubeMesh, p.armorPlateColor)
                matrixStack.pop()
            }
        }

        // 2. Character-Specific Torso with 3D Core & Attachments
        matrixStack.push()
        matrixStack.translate(0f, legBaseY + p.torsoHeight / 2f, 0f)
        matrixStack.rotate(p.torsoTwist, 0f, 1f, 0f)

        // Main Torso Core
        matrixStack.push()
        matrixStack.scale(p.torsoWidth, p.torsoHeight, p.torsoDepth)
        drawMesh(cubeMesh, p.suitPrimaryColor)
        matrixStack.pop()

        // Front Chest Armor & Badges per Character Variant
        when (p.model) {
            CharacterModelId.TITAN -> {
                // Heavy Armored Exo-Suit Blast Plate
                matrixStack.push()
                matrixStack.translate(0f, 0.04f, -p.torsoDepth / 2f - 0.04f)
                matrixStack.scale(p.torsoWidth * 0.92f, p.torsoHeight * 0.76f, 0.08f)
                drawMesh(cubeMesh, p.armorPlateColor)
                matrixStack.pop()

                // Glowing Reactor Grill (3 horizontal radiant power bars)
                for (bar in -1..1) {
                    matrixStack.push()
                    matrixStack.translate(0f, 0.06f + bar * 0.08f, -p.torsoDepth / 2f - 0.085f)
                    matrixStack.scale(p.torsoWidth * 0.50f, 0.035f, 0.02f)
                    drawMesh(cubeMesh, p.neonGlowColor, emissive = 1.0f)
                    matrixStack.pop()
                }
            }
            CharacterModelId.VALKYRIE -> {
                // Swept Aerodynamic Carbon Breastplate
                matrixStack.push()
                matrixStack.translate(0f, 0.05f, -p.torsoDepth / 2f - 0.02f)
                matrixStack.scale(p.torsoWidth * 0.80f, p.torsoHeight * 0.65f, 0.05f)
                drawMesh(cubeMesh, p.armorPlateColor)
                matrixStack.pop()

                // High-velocity Chevron Air Intake
                matrixStack.push()
                matrixStack.translate(0f, 0.10f, -p.torsoDepth / 2f - 0.05f)
                matrixStack.scale(0.12f, 0.12f, 0.02f)
                drawMesh(cubeMesh, p.neonGlowColor, emissive = 1.0f)
                matrixStack.pop()
            }
            CharacterModelId.PHANTOM -> {
                // Stealth Shinobi Vest with Crossed Harness
                matrixStack.push()
                matrixStack.translate(0f, 0.04f, -p.torsoDepth / 2f - 0.02f)
                matrixStack.scale(p.torsoWidth * 0.82f, p.torsoHeight * 0.70f, 0.04f)
                drawMesh(cubeMesh, p.armorPlateColor)
                matrixStack.pop()

                // Glowing Clan Insignia Slit
                matrixStack.push()
                matrixStack.translate(0f, 0.12f, -p.torsoDepth / 2f - 0.045f)
                matrixStack.scale(0.04f, 0.16f, 0.02f)
                drawMesh(cubeMesh, p.neonGlowColor, emissive = 1.0f)
                matrixStack.pop()
            }
            CharacterModelId.CHRONOS -> {
                // Imperial Ornate Gold Breastplate with Orbiting Crystal Core
                matrixStack.push()
                matrixStack.translate(0f, 0.05f, -p.torsoDepth / 2f - 0.025f)
                matrixStack.scale(p.torsoWidth * 0.88f, p.torsoHeight * 0.72f, 0.06f)
                drawMesh(cubeMesh, p.armorPlateColor)
                matrixStack.pop()

                // Floating Rotating Solar Energy Crystal Core
                matrixStack.push()
                matrixStack.translate(0f, 0.10f, -p.torsoDepth / 2f - 0.08f)
                matrixStack.rotate(totalTime * 90f, 0f, 1f, 0.5f)
                matrixStack.scale(0.16f, 0.22f, 0.16f)
                drawMesh(crystalMesh, p.neonGlowColor, emissive = 1.0f)
                matrixStack.pop()
            }
            CharacterModelId.VANGUARD -> {
                // Cyber Infiltrator Chest Armor Plate & Glowing Badge
                matrixStack.push()
                matrixStack.translate(0f, 0.06f, -p.torsoDepth / 2f - 0.02f)
                matrixStack.scale(p.torsoWidth * 0.85f, p.torsoHeight * 0.70f, 0.06f)
                drawMesh(cubeMesh, p.armorPlateColor)
                matrixStack.pop()

                matrixStack.push()
                matrixStack.translate(0f, 0.12f, -p.torsoDepth / 2f - 0.055f)
                matrixStack.scale(0.14f, 0.14f, 0.02f)
                drawMesh(cubeMesh, p.neonGlowColor, emissive = 1.0f)
                matrixStack.pop()
            }
        }

        // Back Mounts & Attachments (Wings, Katanas, Quad Turbines, Solar Fins)
        when (p.model) {
            CharacterModelId.TITAN -> {
                // Quad Turbine Exhaust Engine Block
                matrixStack.push()
                matrixStack.translate(0f, 0.06f, p.torsoDepth / 2f + 0.12f)
                matrixStack.scale(p.torsoWidth * 0.85f, p.torsoHeight * 0.65f, 0.22f)
                drawMesh(cubeMesh, p.armorPlateColor)
                matrixStack.pop()

                // 4 Exhaust Ports (2x2 grid)
                for (idx in 0 until 4) {
                    val ox = PORT_OFFSETS_X[idx]
                    val oy = PORT_OFFSETS_Y[idx]
                    matrixStack.push()
                    matrixStack.translate(ox, oy, p.torsoDepth / 2f + 0.24f)
                    matrixStack.scale(0.10f, 0.10f, 0.05f)
                    drawMesh(cubeMesh, p.neonGlowColor, emissive = 1.0f)
                    matrixStack.pop()
                }
            }
            CharacterModelId.VALKYRIE -> {
                // Dual Swept Kinetic Glider Wings!
                val wingTilt = p.steerBankTilt * 0.35f
                val wingFlap = sin(p.runAnimationTime * 1.5f) * 3f

                // Left Wing
                matrixStack.push()
                matrixStack.translate(-0.08f, 0.10f, p.torsoDepth / 2f + 0.06f)
                matrixStack.rotate(180f, 0f, 1f, 0f)
                matrixStack.rotate(-15f + wingTilt + wingFlap, 0f, 0f, 1f)
                matrixStack.scale(0.95f, 0.95f, 0.95f)
                drawMesh(wingBladeMesh, p.suitPrimaryColor)

                // Left Wing Neon Trailing Strip
                matrixStack.push()
                matrixStack.translate(0.45f, 0.02f, 0.12f)
                matrixStack.scale(0.85f, 0.02f, 0.03f)
                drawMesh(cubeMesh, p.neonGlowColor, emissive = 1.0f)
                matrixStack.pop()
                matrixStack.pop()

                // Right Wing
                matrixStack.push()
                matrixStack.translate(0.08f, 0.10f, p.torsoDepth / 2f + 0.06f)
                matrixStack.rotate(15f - wingTilt - wingFlap, 0f, 0f, 1f)
                matrixStack.scale(0.95f, 0.95f, 0.95f)
                drawMesh(wingBladeMesh, p.suitPrimaryColor)

                // Right Wing Neon Trailing Strip
                matrixStack.push()
                matrixStack.translate(0.45f, 0.02f, 0.12f)
                matrixStack.scale(0.85f, 0.02f, 0.03f)
                drawMesh(cubeMesh, p.neonGlowColor, emissive = 1.0f)
                matrixStack.pop()
                matrixStack.pop()

                // Dual Micro-Afterburners
                for (ox in WING_OFFSETS) {
                    matrixStack.push()
                    matrixStack.translate(ox, -0.10f, p.torsoDepth / 2f + 0.12f)
                    matrixStack.scale(0.08f, 0.08f, 0.14f)
                    drawMesh(cubeMesh, p.armorPlateColor)
                    matrixStack.pop()

                    matrixStack.push()
                    matrixStack.translate(ox, -0.10f, p.torsoDepth / 2f + 0.20f)
                    matrixStack.scale(0.06f, 0.06f, 0.03f)
                    drawMesh(cubeMesh, p.neonGlowColor, emissive = 1.0f)
                    matrixStack.pop()
                }
            }
            CharacterModelId.PHANTOM -> {
                // Dual Crossed Cyber Katanas in 'X' Scabbards on Back
                matrixStack.push()
                matrixStack.translate(0f, 0.08f, p.torsoDepth / 2f + 0.08f)
                matrixStack.rotate(32f, 0f, 0f, 1f)
                matrixStack.scale(0.06f, 0.95f, 0.06f)
                drawMesh(cubeMesh, p.armorPlateColor)
                matrixStack.push()
                matrixStack.translate(0f, 0.52f, 0f)
                matrixStack.scale(1.3f, 0.18f, 1.3f)
                drawMesh(cubeMesh, p.neonGlowColor, emissive = 1.0f)
                matrixStack.pop()
                matrixStack.pop()

                matrixStack.push()
                matrixStack.translate(0f, 0.08f, p.torsoDepth / 2f + 0.08f)
                matrixStack.rotate(-32f, 0f, 0f, 1f)
                matrixStack.scale(0.06f, 0.95f, 0.06f)
                drawMesh(cubeMesh, p.armorPlateColor)
                matrixStack.push()
                matrixStack.translate(0f, 0.52f, 0f)
                matrixStack.scale(1.3f, 0.18f, 1.3f)
                drawMesh(cubeMesh, p.neonGlowColor, emissive = 1.0f)
                matrixStack.pop()
                matrixStack.pop()

                // Dynamic Kinetic Wind Scarf Trailing Behind Neck!
                val scarfSegs = 4
                var currZ = p.torsoDepth / 2f + 0.08f
                var currY = p.torsoHeight / 2f - 0.04f
                for (s in 0 until scarfSegs) {
                    val wave = sin(totalTime * 14f - s * 0.8f) * 0.06f
                    matrixStack.push()
                    matrixStack.translate(wave, currY, currZ)
                    matrixStack.scale(0.18f - s * 0.02f, 0.04f, 0.22f)
                    drawMesh(cubeMesh, p.neonGlowColor, emissive = 0.85f)
                    matrixStack.pop()
                    currZ += 0.18f
                    currY -= 0.05f
                }
            }
            CharacterModelId.CHRONOS -> {
                // Celestial Back Solar Fin Array
                matrixStack.push()
                matrixStack.translate(0f, 0.05f, p.torsoDepth / 2f + 0.06f)
                matrixStack.scale(p.torsoWidth * 0.70f, p.torsoHeight * 0.75f, 0.08f)
                drawMesh(cubeMesh, p.armorPlateColor)
                matrixStack.pop()

                // 4 Radiant Solar Winglet Tabs
                for (ang in FIN_ANGLES) {
                    matrixStack.push()
                    matrixStack.translate(0f, 0.12f, p.torsoDepth / 2f + 0.10f)
                    matrixStack.rotate(ang, 0f, 0f, 1f)
                    matrixStack.translate(0f, 0.38f, 0f)
                    matrixStack.scale(0.05f, 0.30f, 0.03f)
                    drawMesh(cubeMesh, p.neonGlowColor, emissive = 1.0f)
                    matrixStack.pop()
                }
            }
            CharacterModelId.VANGUARD -> {
                // Cyber Pack & Twin Luminous Neon Cyan Thruster Panels on Back (matches reference art!)
                matrixStack.push()
                matrixStack.translate(0f, 0.04f, p.torsoDepth / 2f + 0.04f)
                matrixStack.scale(0.36f, 0.54f, 0.08f)
                drawMesh(cubeMesh, p.armorPlateColor)
                matrixStack.pop()

                // Left Glowing Neon Cyan Thruster Panel
                matrixStack.push()
                matrixStack.translate(-0.09f, 0.06f, p.torsoDepth / 2f + 0.085f)
                matrixStack.scale(0.09f, 0.28f, 0.02f)
                drawMesh(cubeMesh, p.neonGlowColor, emissive = 1.0f)
                matrixStack.pop()

                // Right Glowing Neon Cyan Thruster Panel
                matrixStack.push()
                matrixStack.translate(0.09f, 0.06f, p.torsoDepth / 2f + 0.085f)
                matrixStack.scale(0.09f, 0.28f, 0.02f)
                drawMesh(cubeMesh, p.neonGlowColor, emissive = 1.0f)
                matrixStack.pop()

                // Lower Glowing Cyan Belt Accent
                matrixStack.push()
                matrixStack.translate(0f, -0.15f, p.torsoDepth / 2f + 0.085f)
                matrixStack.scale(0.26f, 0.045f, 0.02f)
                drawMesh(cubeMesh, p.neonGlowColor, emissive = 1.0f)
                matrixStack.pop()
            }
        }

        matrixStack.pop() // End Torso

        // 3. Sculpted Head & Helmets (Halo, Horns, Dual Optics, Flight Visor)
        val headY = legBaseY + p.torsoHeight + p.headSize / 2f + 0.04f
        matrixStack.push()
        matrixStack.translate(0f, headY, 0f)

        // Head/Face Core
        matrixStack.push()
        matrixStack.scale(p.headSize * 0.85f, p.headSize, p.headSize * 0.88f)
        drawMesh(cubeMesh, if (p.model == CharacterModelId.TITAN) p.armorPlateColor else p.skinColor)
        matrixStack.pop()

        when (p.model) {
            CharacterModelId.TITAN -> {
                // Heavy Brow Plate & Enclosed Combat Helmet
                matrixStack.push()
                matrixStack.translate(0f, p.headSize * 0.25f, 0f)
                matrixStack.scale(p.headSize * 1.05f, p.headSize * 0.60f, p.headSize * 1.05f)
                drawMesh(cubeMesh, p.suitSecondaryColor)
                matrixStack.pop()

                // Heavy Brow Overhang
                matrixStack.push()
                matrixStack.translate(0f, p.headSize * 0.16f, -p.headSize * 0.44f - 0.04f)
                matrixStack.scale(p.headSize * 0.95f, 0.12f, 0.10f)
                drawMesh(cubeMesh, p.armorPlateColor)
                matrixStack.pop()

                // Dual Glowing Red/Amber Optical Slits
                for (side in SIDE_AMMUNITION) {
                    matrixStack.push()
                    matrixStack.translate(side, 0.02f, -p.headSize * 0.44f - 0.03f)
                    matrixStack.scale(0.08f, 0.045f, 0.04f)
                    drawMesh(cubeMesh, p.visorColor, emissive = 1.0f)
                    matrixStack.pop()
                }

                // Heavy Chin Rebreather Grill
                matrixStack.push()
                matrixStack.translate(0f, -p.headSize * 0.28f, -p.headSize * 0.44f - 0.02f)
                matrixStack.scale(p.headSize * 0.60f, 0.16f, 0.08f)
                drawMesh(cubeMesh, p.armorPlateColor)
                matrixStack.pop()
            }
            CharacterModelId.VALKYRIE -> {
                // Flight Helmet with Swept Side Aero-Fins
                matrixStack.push()
                matrixStack.translate(0f, p.headSize * 0.22f, p.headSize * 0.06f)
                matrixStack.scale(p.headSize * 0.95f, p.headSize * 0.55f, p.headSize * 0.95f)
                drawMesh(cubeMesh, p.suitSecondaryColor)
                matrixStack.pop()

                // Dual Swept Side Aero Antenna Fins
                for (side in SIDE_OFFSETS) {
                    matrixStack.push()
                    matrixStack.translate(side * (p.headSize * 0.48f), p.headSize * 0.18f, p.headSize * 0.10f)
                    matrixStack.rotate(side * 22f, 0f, 0f, 1f)
                    matrixStack.rotate(-28f, 1f, 0f, 0f)
                    matrixStack.scale(0.035f, 0.28f, 0.10f)
                    drawMesh(cubeMesh, p.neonGlowColor, emissive = 1.0f)
                    matrixStack.pop()
                }

                // Panoramic Wraparound Flight Visor
                matrixStack.push()
                matrixStack.translate(0f, 0.02f, -p.headSize * 0.44f - 0.02f)
                matrixStack.scale(p.headSize * 0.88f, 0.18f, 0.06f)
                drawMesh(cubeMesh, p.visorColor, emissive = 1.0f)
                matrixStack.pop()
            }
            CharacterModelId.PHANTOM -> {
                // Stealth Ninja Cowl with Dual Sharp Cowl Horns
                matrixStack.push()
                matrixStack.translate(0f, p.headSize * 0.22f, 0f)
                matrixStack.scale(p.headSize * 0.98f, p.headSize * 0.60f, p.headSize * 0.98f)
                drawMesh(cubeMesh, p.suitSecondaryColor)
                matrixStack.pop()

                // Dual Ninja Cowl Horns / Ears
                for (side in SIDE_OFFSETS) {
                    matrixStack.push()
                    matrixStack.translate(side * (p.headSize * 0.35f), p.headSize * 0.55f, 0f)
                    matrixStack.rotate(side * 18f, 0f, 0f, 1f)
                    matrixStack.scale(0.08f, 0.24f, 0.12f)
                    drawMesh(wedgeMesh, p.armorPlateColor)
                    matrixStack.pop()
                }

                // Horizontal Ninja Mono-Eye Visor
                matrixStack.push()
                matrixStack.translate(0f, 0.03f, -p.headSize * 0.44f - 0.02f)
                matrixStack.scale(p.headSize * 0.75f, 0.07f, 0.05f)
                drawMesh(cubeMesh, p.visorColor, emissive = 1.0f)
                matrixStack.pop()

                // Stealth Faceplate
                matrixStack.push()
                matrixStack.translate(0f, -p.headSize * 0.25f, -p.headSize * 0.44f - 0.01f)
                matrixStack.scale(p.headSize * 0.70f, 0.18f, 0.05f)
                drawMesh(cubeMesh, p.armorPlateColor)
                matrixStack.pop()
            }
            CharacterModelId.CHRONOS -> {
                // Celestial Sovereign Helmet
                matrixStack.push()
                matrixStack.translate(0f, p.headSize * 0.25f, 0f)
                matrixStack.scale(p.headSize * 0.96f, p.headSize * 0.58f, p.headSize * 0.96f)
                drawMesh(cubeMesh, p.armorPlateColor)
                matrixStack.pop()

                // Radiant Face Visor
                matrixStack.push()
                matrixStack.translate(0f, 0.03f, -p.headSize * 0.44f - 0.02f)
                matrixStack.scale(p.headSize * 0.82f, 0.16f, 0.05f)
                drawMesh(cubeMesh, p.visorColor, emissive = 1.0f)
                matrixStack.pop()

                // *** FLOATING CELESTIAL GOLDEN HALO TORUS ***
                matrixStack.push()
                matrixStack.translate(0f, p.headSize * 0.75f, 0.02f)
                matrixStack.rotate(15f, 1f, 0f, 0f)
                matrixStack.rotate(totalTime * 50f, 0f, 1f, 0f)
                matrixStack.scale(0.36f, 0.06f, 0.36f)
                drawMesh(torusRingMesh, p.neonGlowColor, emissive = 1.0f)
                matrixStack.pop()
            }
            CharacterModelId.VANGUARD -> {
                // Aerodynamic Helmet with Crest Fin
                matrixStack.push()
                matrixStack.translate(0f, p.headSize * 0.22f, p.headSize * 0.08f)
                matrixStack.scale(p.headSize * 0.95f, p.headSize * 0.55f, p.headSize * 0.95f)
                drawMesh(cubeMesh, p.hairColor)
                matrixStack.pop()

                // Central Aero Crest Fin
                matrixStack.push()
                matrixStack.translate(0f, p.headSize * 0.55f, 0f)
                matrixStack.scale(0.04f, 0.14f, p.headSize * 0.85f)
                drawMesh(cubeMesh, p.neonGlowColor, emissive = 1.0f)
                matrixStack.pop()

                // Glowing Visor
                matrixStack.push()
                matrixStack.translate(0f, 0.02f, -p.headSize * 0.44f - 0.02f)
                matrixStack.scale(p.headSize * 0.82f, 0.14f, 0.05f)
                drawMesh(cubeMesh, p.visorColor, emissive = 1.0f)
                matrixStack.pop()
            }
        }

        matrixStack.pop() // End Head

        // 4. Articulated Legs
        val hipOffsetX = if (p.model == CharacterModelId.TITAN) 0.19f else 0.15f

        // LEFT LEG
        matrixStack.push()
        matrixStack.translate(-hipOffsetX, legBaseY, 0f)
        matrixStack.rotate(p.thighSwingLeft, 1f, 0f, 0f)

        // Left Thigh
        matrixStack.push()
        matrixStack.translate(0f, -p.thighLen / 2f, 0f)
        matrixStack.scale(p.legThick, p.thighLen, p.legThick)
        drawMesh(cubeMesh, p.suitSecondaryColor)
        matrixStack.pop()

        // Left Knee Joint & Shin
        matrixStack.push()
        matrixStack.translate(0f, -p.thighLen, 0f)
        matrixStack.rotate(-p.kneeBendLeft, 1f, 0f, 0f)

        // Knee Armor Cap
        matrixStack.push()
        matrixStack.translate(0f, 0f, -p.legThick * 0.55f)
        val kneeCapScale = if (p.model == CharacterModelId.TITAN) 1.35f else 1.1f
        matrixStack.scale(p.legThick * kneeCapScale, p.legThick * 0.85f, 0.08f)
        drawMesh(cubeMesh, p.armorPlateColor)
        matrixStack.pop()

        // Shin / Calf
        matrixStack.push()
        matrixStack.translate(0f, -p.shinLen / 2f, 0f)
        matrixStack.scale(p.legThick * 0.92f, p.shinLen, p.legThick * 0.92f)
        drawMesh(cubeMesh, p.suitPrimaryColor)
        matrixStack.pop()

        // Left Athletic Running Sneaker / Heavy Boot
        val bootLen = if (p.model == CharacterModelId.TITAN) 0.40f else 0.32f
        val bootW = if (p.model == CharacterModelId.TITAN) p.legThick * 1.25f else p.legThick * 1.05f
        matrixStack.push()
        matrixStack.translate(0f, -p.shinLen + 0.04f, -0.05f)
        matrixStack.scale(bootW, 0.10f, bootLen)
        drawMesh(cubeMesh, p.suitSecondaryColor)
        matrixStack.pop()

        // Glowing Neon Sneaker Sole
        matrixStack.push()
        matrixStack.translate(0f, -p.shinLen, -0.05f)
        matrixStack.scale(bootW * 1.05f, 0.03f, bootLen * 1.05f)
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
        matrixStack.scale(p.legThick * kneeCapScale, p.legThick * 0.85f, 0.08f)
        drawMesh(cubeMesh, p.armorPlateColor)
        matrixStack.pop()

        // Shin / Calf
        matrixStack.push()
        matrixStack.translate(0f, -p.shinLen / 2f, 0f)
        matrixStack.scale(p.legThick * 0.92f, p.shinLen, p.legThick * 0.92f)
        drawMesh(cubeMesh, p.suitPrimaryColor)
        matrixStack.pop()

        // Right Athletic Running Sneaker / Heavy Boot
        matrixStack.push()
        matrixStack.translate(0f, -p.shinLen + 0.04f, -0.05f)
        matrixStack.scale(bootW, 0.10f, bootLen)
        drawMesh(cubeMesh, p.suitSecondaryColor)
        matrixStack.pop()

        // Glowing Neon Sneaker Sole
        matrixStack.push()
        matrixStack.translate(0f, -p.shinLen, -0.05f)
        matrixStack.scale(bootW * 1.05f, 0.03f, bootLen * 1.05f)
        drawMesh(cubeMesh, p.neonGlowColor, emissive = 1.0f)
        matrixStack.pop()

        matrixStack.pop() // End Right Shin
        matrixStack.pop() // End Right Leg

        // 5. Articulated Arms with Sculpted Pauldrons & Gauntlets
        val shoulderY = legBaseY + p.torsoHeight - 0.10f
        val shoulderX = p.torsoWidth / 2f + p.armThick / 2f + 0.03f

        // LEFT ARM
        matrixStack.push()
        matrixStack.translate(-shoulderX, shoulderY, 0f)
        matrixStack.rotate(p.armSwingLeft, 1f, 0f, 0f)

        // Shoulder Pauldron per Character
        when (p.model) {
            CharacterModelId.TITAN -> {
                // Massive Multi-Tier Mech Pauldron
                matrixStack.push()
                matrixStack.translate(0f, 0.04f, 0f)
                matrixStack.scale(p.armThick * 1.9f, 0.20f, p.armThick * 1.8f)
                drawMesh(cubeMesh, p.armorPlateColor)
                // Hazard orange warning accent
                matrixStack.push()
                matrixStack.translate(0f, 0.11f, 0f)
                matrixStack.scale(0.85f, 0.04f, 0.85f)
                drawMesh(cubeMesh, p.neonGlowColor, emissive = 1.0f)
                matrixStack.pop()
                matrixStack.pop()
            }
            CharacterModelId.PHANTOM -> {
                // Razor Angular Stealth Blade Pauldron
                matrixStack.push()
                matrixStack.translate(0f, 0.05f, 0f)
                matrixStack.rotate(-15f, 0f, 0f, 1f)
                matrixStack.scale(p.armThick * 1.4f, 0.22f, p.armThick * 1.5f)
                drawMesh(wedgeMesh, p.armorPlateColor)
                matrixStack.pop()
            }
            CharacterModelId.CHRONOS -> {
                // Imperial Golden Crown Pauldron
                matrixStack.push()
                matrixStack.translate(0f, 0.04f, 0f)
                matrixStack.scale(p.armThick * 1.6f, 0.16f, p.armThick * 1.6f)
                drawMesh(cubeMesh, p.suitPrimaryColor)
                matrixStack.pop()
            }
            else -> {
                matrixStack.push()
                matrixStack.translate(0f, 0.02f, 0f)
                matrixStack.scale(p.armThick * 1.3f, 0.12f, p.armThick * 1.3f)
                drawMesh(cubeMesh, p.armorPlateColor)
                matrixStack.pop()
            }
        }

        // Upper Arm
        matrixStack.push()
        matrixStack.translate(0f, -p.upperArmLen / 2f, 0f)
        matrixStack.scale(p.armThick, p.upperArmLen, p.armThick)
        drawMesh(cubeMesh, p.suitPrimaryColor)
        matrixStack.pop()

        // Forearm
        matrixStack.push()
        matrixStack.translate(0f, -p.upperArmLen, 0f)
        matrixStack.rotate(-p.elbowBendLeft, 1f, 0f, 0f)

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

        // Fist
        matrixStack.push()
        matrixStack.translate(0f, -p.foreArmLen, 0f)
        matrixStack.scale(p.armThick * 1.05f, 0.12f, p.armThick * 1.15f)
        drawMesh(cubeMesh, p.armorPlateColor)
        matrixStack.pop()

        matrixStack.pop() // End Left Forearm
        matrixStack.pop() // End Left Arm

        // RIGHT ARM
        matrixStack.push()
        matrixStack.translate(shoulderX, shoulderY, 0f)
        matrixStack.rotate(p.armSwingRight, 1f, 0f, 0f)

        // Shoulder Pauldron per Character
        when (p.model) {
            CharacterModelId.TITAN -> {
                matrixStack.push()
                matrixStack.translate(0f, 0.04f, 0f)
                matrixStack.scale(p.armThick * 1.9f, 0.20f, p.armThick * 1.8f)
                drawMesh(cubeMesh, p.armorPlateColor)
                matrixStack.push()
                matrixStack.translate(0f, 0.11f, 0f)
                matrixStack.scale(0.85f, 0.04f, 0.85f)
                drawMesh(cubeMesh, p.neonGlowColor, emissive = 1.0f)
                matrixStack.pop()
                matrixStack.pop()
            }
            CharacterModelId.PHANTOM -> {
                matrixStack.push()
                matrixStack.translate(0f, 0.05f, 0f)
                matrixStack.rotate(15f, 0f, 0f, 1f)
                matrixStack.scale(p.armThick * 1.4f, 0.22f, p.armThick * 1.5f)
                drawMesh(wedgeMesh, p.armorPlateColor)
                matrixStack.pop()
            }
            CharacterModelId.CHRONOS -> {
                matrixStack.push()
                matrixStack.translate(0f, 0.04f, 0f)
                matrixStack.scale(p.armThick * 1.6f, 0.16f, p.armThick * 1.6f)
                drawMesh(cubeMesh, p.suitPrimaryColor)
                matrixStack.pop()
            }
            else -> {
                matrixStack.push()
                matrixStack.translate(0f, 0.02f, 0f)
                matrixStack.scale(p.armThick * 1.3f, 0.12f, p.armThick * 1.3f)
                drawMesh(cubeMesh, p.armorPlateColor)
                matrixStack.pop()
            }
        }

        // Upper Arm
        matrixStack.push()
        matrixStack.translate(0f, -p.upperArmLen / 2f, 0f)
        matrixStack.scale(p.armThick, p.upperArmLen, p.armThick)
        drawMesh(cubeMesh, p.suitPrimaryColor)
        matrixStack.pop()

        // Forearm
        matrixStack.push()
        matrixStack.translate(0f, -p.upperArmLen, 0f)
        matrixStack.rotate(-p.elbowBendRight, 1f, 0f, 0f)

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

        // Fist
        matrixStack.push()
        matrixStack.translate(0f, -p.foreArmLen, 0f)
        matrixStack.scale(p.armThick * 1.05f, 0.12f, p.armThick * 1.15f)
        drawMesh(cubeMesh, p.armorPlateColor)
        matrixStack.pop()

        // *** SCI-FI PLASMA BLASTER RIFLE (Mounted on Right Hand) ***
        matrixStack.push()
        matrixStack.translate(0f, -p.foreArmLen + 0.02f, -0.16f)

        // Gun Receiver Body
        matrixStack.push()
        matrixStack.scale(0.13f, 0.16f, 0.42f)
        drawMesh(cubeMesh, floatArrayOf(0.14f, 0.15f, 0.18f, 1f))
        matrixStack.pop()

        // Upper Cooling Rail
        matrixStack.push()
        matrixStack.translate(0f, 0.09f, -0.04f)
        matrixStack.scale(0.08f, 0.04f, 0.36f)
        drawMesh(cubeMesh, floatArrayOf(0.25f, 0.28f, 0.32f, 1f))
        matrixStack.pop()

        // Glowing Plasma Core Side Vents
        for (side in listOf(-1f, 1f)) {
            matrixStack.push()
            matrixStack.translate(side * 0.07f, 0.01f, -0.04f)
            matrixStack.scale(0.015f, 0.08f, 0.24f)
            drawMesh(cubeMesh, floatArrayOf(0.0f, 0.95f, 1.0f, 1f), emissive = 1.0f)
            matrixStack.pop()
        }

        // Heavy Plasma Barrel extending forward towards incoming balls
        matrixStack.push()
        matrixStack.translate(0f, 0.01f, -0.32f)
        matrixStack.scale(0.095f, 0.095f, 0.26f)
        drawMesh(cubeMesh, floatArrayOf(0.09f, 0.10f, 0.12f, 1f))
        matrixStack.pop()

        // Glowing Muzzle Ring (flashes bright white on shot with zero player obstruction)
        val muzzleRingColor = if (p.muzzleFlashTimer > 0f) {
            floatArrayOf(1.0f, 1.0f, 1.0f, 1f)
        } else {
            floatArrayOf(0.0f, 0.95f, 1.0f, 1f)
        }
        matrixStack.push()
        matrixStack.translate(0f, 0.01f, -0.46f)
        matrixStack.scale(0.11f, 0.11f, 0.05f)
        drawMesh(cubeMesh, muzzleRingColor, emissive = 1.0f)
        matrixStack.pop()

        matrixStack.pop() // End Blaster

        matrixStack.pop() // End Right Forearm
        matrixStack.pop() // End Right Arm

        // 2x SPEED Boost Mode: Luminous Warp Streaks & Jet Aura (Matches Boost Mode panel in image!)
        if (p.isBoosting) {
            val boostPulse = kotlin.math.sin(totalTime * 25f) * 0.15f + 0.85f
            for (i in 0 until 8) {
                val ang = (i.toFloat() / 8f) * 2f * kotlin.math.PI.toFloat()
                val rx = kotlin.math.cos(ang) * (0.42f + (i % 2) * 0.18f)
                val ry = kotlin.math.sin(ang) * (0.50f + (i % 2) * 0.22f) + 0.9f
                matrixStack.push()
                matrixStack.translate(rx, ry, 0.4f + (i % 3) * 0.35f)
                matrixStack.scale(0.04f, 0.04f, 2.2f)
                drawMesh(cubeMesh, floatArrayOf(0.0f, 0.95f, 1.0f, boostPulse), emissive = 1.0f)
                matrixStack.pop()
            }
        }

        // CHRONO OVERDRIVE: Supersonic Golden Aura & Prismatic Energy Fields
        if (p.isOverdriveActive) {
            val auraPulse = kotlin.math.sin(totalTime * 18f) * 0.15f + 0.85f
            // Outer Rotating Energy Torus Ring
            matrixStack.push()
            matrixStack.translate(0f, 0.95f, 0f)
            matrixStack.rotate(totalTime * 120f, 0f, 1f, 0f)
            matrixStack.rotate(25f, 1f, 0f, 0f)
            matrixStack.scale(1.4f, 1.4f, 1.4f)
            drawMesh(torusRingMesh, floatArrayOf(1.0f, 0.85f, 0.15f, auraPulse), emissive = 1.0f)
            matrixStack.pop()

            // Counter-rotating Inner Cyan Ring
            matrixStack.push()
            matrixStack.translate(0f, 0.95f, 0f)
            matrixStack.rotate(-totalTime * 160f, 0f, 1f, 0f)
            matrixStack.rotate(-25f, 1f, 0f, 0f)
            matrixStack.scale(1.15f, 1.15f, 1.15f)
            drawMesh(torusRingMesh, floatArrayOf(0.0f, 0.95f, 1.0f, auraPulse), emissive = 1.0f)
            matrixStack.pop()

            // Golden Warp Streaks
            for (i in 0 until 10) {
                val ang = (i.toFloat() / 10f) * 2f * kotlin.math.PI.toFloat()
                val rx = kotlin.math.cos(ang) * (0.50f + (i % 2) * 0.22f)
                val ry = kotlin.math.sin(ang) * (0.55f + (i % 2) * 0.25f) + 0.95f
                matrixStack.push()
                matrixStack.translate(rx, ry, 0.4f + (i % 3) * 0.45f)
                matrixStack.scale(0.05f, 0.05f, 2.8f)
                drawMesh(cubeMesh, floatArrayOf(1.0f, 0.85f, 0.2f, auraPulse), emissive = 1.0f)
                matrixStack.pop()
            }
        }

        // Cyber Thruster Dash Phantom Ghost Trail
        if (p.isDashing) {
            val dashAlpha = (p.dashTimer / 0.26f).coerceIn(0.2f, 0.8f)
            matrixStack.push()
            matrixStack.translate(-p.dashDirection * 0.35f, 0f, 0.45f)
            matrixStack.scale(0.88f, 0.88f, 0.88f)
            drawMesh(cubeMesh, floatArrayOf(0.0f, 0.95f, 1.0f, dashAlpha * 0.5f), emissive = 1.0f)
            matrixStack.pop()
        }

        matrixStack.pop() // End Player
    }

    private fun renderProjectiles() {
        val projs = physics.projectilePool
        var anyActive = false
        for (pr in projs) {
            if (pr.isActive) {
                anyActive = true
                break
            }
        }
        if (!anyActive) return

        // 1. Render glowing bullet heads
        for (pr in projs) {
            if (!pr.isActive) continue
            matrixStack.push()
            matrixStack.translate(pr.position.x, pr.position.y, pr.position.z)

            // Inner super-bright incandescent white energy core
            matrixStack.push()
            matrixStack.scale(pr.radius * 0.38f, pr.radius * 0.38f, 0.95f)
            drawMesh(cubeMesh, WHITE_COLOR, emissive = 1.0f)
            matrixStack.pop()

            // Outer radiant cyan plasma bolt
            matrixStack.push()
            matrixStack.scale(pr.radius * 0.75f, pr.radius * 0.75f, 1.45f)
            drawMesh(cubeMesh, pr.color, emissive = 1.0f)
            matrixStack.pop()

            matrixStack.pop()
        }

        // 2. Render sleek streamlined plasma trail streaming strictly behind each bullet
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glDepthMask(false)

        for (pr in projs) {
            if (!pr.isActive) continue
            val distTraveled = (pr.startZ - pr.position.z).coerceAtLeast(0f)
            if (distTraveled <= 0.1f) continue

            for (i in TRAIL_OFFSETS.indices) {
                val offsetZ = TRAIL_OFFSETS[i]
                val baseLen = TRAIL_BASE_LENS[i]
                val alpha = TRAIL_ALPHAS[i]
                if (offsetZ - baseLen / 2f >= distTraveled) continue

                // Clamp trail segment so it NEVER extends behind the firing muzzle / player
                val clampedLen = minOf(baseLen, (distTraveled - (offsetZ - baseLen / 2f)).coerceAtLeast(0.1f))
                val segZ = pr.position.z + offsetZ
                if (segZ > pr.startZ) continue // Strictly forward of the firing point

                val taper = 1.0f - (i.toFloat() / TRAIL_OFFSETS.size.toFloat()) * 0.65f
                val thickness = pr.radius * 0.55f * taper

                matrixStack.push()
                matrixStack.translate(pr.position.x, pr.position.y, segZ)
                matrixStack.scale(thickness, thickness, clampedLen)
                drawMesh(cubeMesh, 0.0f, 0.95f, 1.0f, alpha, emissive = 0.95f)

                // Thin luminous center core for the frontmost trail segment
                if (i == 0) {
                    matrixStack.scale(0.45f, 0.45f, 0.85f)
                    drawMesh(cubeMesh, 1.0f, 1.0f, 1.0f, alpha * 0.85f, emissive = 1.0f)
                }
                matrixStack.pop()
            }
        }

        GLES20.glDepthMask(true)
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    private fun renderParticles() {
        val particles = physics.particles.particles
        var anyActive = false
        for (p in particles) {
            if (p.lifetime > 0f) {
                anyActive = true
                break
            }
        }
        if (!anyActive) return

        // Enable hardware alpha blending and disable depth writing so particles never occlude 3D obstacles
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glDepthMask(false)

        for (pt in particles) {
            if (pt.lifetime <= 0f) continue
            matrixStack.push()
            matrixStack.translate(pt.position.x, pt.position.y, pt.position.z)

            when (pt.particleType) {
                ParticleType.DUST_CLOUD -> {
                    // Soft semi-transparent billowing road dust sphere
                    // As it ages, it expands outwards into a soft ground-hugging dust cloud
                    val lifeProgress = (pt.lifetime / pt.maxLife).coerceIn(0f, 1f)
                    val scaleFactor = pt.size * (0.85f + 0.65f * (1f - lifeProgress))
                    matrixStack.rotate(pt.rotation, 0f, 1f, 0f)
                    matrixStack.scale(scaleFactor, scaleFactor * 0.38f, scaleFactor)
                    drawMesh(smoothSphereMesh, pt.color, emissive = 0.08f)
                }
                ParticleType.ROCK_DEBRIS -> {
                    // Tumbling geometric gravel / concrete fragment
                    matrixStack.rotate(pt.rotation, 0.75f, 0.55f, 0.35f)
                    matrixStack.scale(pt.size * 1.15f, pt.size * 0.75f, pt.size * 0.90f)
                    drawMesh(cubeMesh, pt.color, emissive = 0.15f)
                }
                ParticleType.SHOCKWAVE -> {
                    // Expanding flat ground energy shockwave
                    matrixStack.scale(pt.size, 0.04f, pt.size)
                    drawMesh(torusRingMesh, pt.color, emissive = 0.85f)
                }
                ParticleType.SPARK -> {
                    // High-energy kinetic spark
                    matrixStack.rotate(pt.rotation, 0f, 0f, 1f)
                    matrixStack.scale(pt.size * 0.45f, pt.size * 1.1f, pt.size * 0.45f)
                    drawMesh(cubeMesh, pt.color, emissive = 1.0f)
                }
                ParticleType.SPEED_STREAK -> {
                    // Elongated speed slipstream streak
                    matrixStack.scale(pt.size * 0.28f, pt.size * 0.28f, pt.size * 2.6f)
                    drawMesh(cubeMesh, pt.color, emissive = 0.90f)
                }
                ParticleType.CYBER_GHOST -> {
                    // Semi-transparent athletic ghost silhouette with glowing neon edges
                    val lifeProgress = (pt.lifetime / pt.maxLife).coerceIn(0f, 1f)
                    matrixStack.scale(pt.size * 0.85f, pt.size * 1.8f, pt.size * 0.65f)
                    val ghostColor = floatArrayOf(pt.color[0], pt.color[1], pt.color[2], (pt.color[3] * lifeProgress).coerceIn(0f, 1f))
                    drawMesh(cubeMesh, ghostColor, emissive = 1.0f)
                }
                ParticleType.OVERDRIVE_BOLT -> {
                    // Crackling electric energy bolt
                    val lifeProgress = (pt.lifetime / pt.maxLife).coerceIn(0f, 1f)
                    matrixStack.rotate(pt.rotation, 0.4f, 0.8f, 0.2f)
                    matrixStack.scale(pt.size * 0.35f, pt.size * 1.4f, pt.size * 0.35f)
                    val boltColor = floatArrayOf(pt.color[0], pt.color[1], pt.color[2], (pt.color[3] * lifeProgress).coerceIn(0f, 1f))
                    drawMesh(cubeMesh, boltColor, emissive = 1.0f)
                }
            }
            matrixStack.pop()
        }

        // Restore default depth writing and blending state
        GLES20.glDepthMask(true)
        GLES20.glDisable(GLES20.GL_BLEND)
    }
}

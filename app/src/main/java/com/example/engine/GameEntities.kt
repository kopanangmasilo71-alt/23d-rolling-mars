package com.example.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

enum class BallType(val displayName: String, val baseSpeedMult: Float, val radius: Float) {
    STRAIGHT("Straight", 1.0f, 0.75f),
    LEFT_TO_RIGHT("Crosser Right", 1.05f, 0.75f),
    RIGHT_TO_LEFT("Crosser Left", 1.05f, 0.75f),
    FAST("Speedster", 1.45f, 0.65f),
    GIANT("Juggernaut", 0.8f, 1.35f),
    BOUNCING("Bouncer", 1.1f, 0.75f)
}

class RollingBall(val id: Int) {
    var isActive: Boolean = false
    val position = Vector3()
    var forwardVelocity: Float = 0f
    var horizontalVelocity: Float = 0f
    var radius: Float = 0.75f
    var rollAngleX: Float = 0f
    var rollAngleZ: Float = 0f
    var bouncePhase: Float = 0f
    var bounceAmp: Float = 0f
    var ballType: BallType = BallType.STRAIGHT
    var hasDodged: Boolean = false

    // Color palette for this ball
    var colorA = floatArrayOf(0.95f, 0.25f, 0.2f, 1f)
    var colorB = floatArrayOf(0.98f, 0.85f, 0.2f, 1f)

    fun reset(
        type: BallType,
        spawnX: Float,
        spawnZ: Float,
        baseSpeed: Float,
        lateralSpeed: Float = 0f
    ) {
        ballType = type
        isActive = true
        radius = type.radius
        position.set(spawnX, radius, spawnZ)
        forwardVelocity = baseSpeed * type.baseSpeedMult
        horizontalVelocity = lateralSpeed
        rollAngleX = 0f
        rollAngleZ = 0f
        bouncePhase = 0f
        bounceAmp = if (type == BallType.BOUNCING) 0.6f else 0f
        hasDodged = false

        when (type) {
            BallType.STRAIGHT -> {
                colorA = floatArrayOf(0.92f, 0.35f, 0.15f, 1f)
                colorB = floatArrayOf(1.0f, 0.85f, 0.25f, 1f)
            }
            BallType.LEFT_TO_RIGHT -> {
                colorA = floatArrayOf(0.15f, 0.75f, 0.95f, 1f)
                colorB = floatArrayOf(0.9f, 0.95f, 1.0f, 1f)
            }
            BallType.RIGHT_TO_LEFT -> {
                colorA = floatArrayOf(0.85f, 0.2f, 0.85f, 1f)
                colorB = floatArrayOf(1.0f, 0.7f, 0.95f, 1f)
            }
            BallType.FAST -> {
                colorA = floatArrayOf(1.0f, 0.12f, 0.12f, 1f)
                colorB = floatArrayOf(1.0f, 0.95f, 0.1f, 1f)
            }
            BallType.GIANT -> {
                colorA = floatArrayOf(0.35f, 0.35f, 0.45f, 1f)
                colorB = floatArrayOf(0.75f, 0.45f, 0.25f, 1f)
            }
            BallType.BOUNCING -> {
                colorA = floatArrayOf(0.25f, 0.85f, 0.35f, 1f)
                colorB = floatArrayOf(0.9f, 1.0f, 0.3f, 1f)
            }
        }
    }

    fun update(dt: Float) {
        if (!isActive) return

        // Move along Z toward positive Z (where player is located around Z=0)
        position.z += forwardVelocity * dt
        position.x += horizontalVelocity * dt

        // Roll rotation: circumference = 2 * PI * r
        // Angular change = (distance / radius) radians -> degrees
        val rollStepX = (forwardVelocity * dt / radius) * (180f / PI.toFloat())
        rollAngleX = (rollAngleX + rollStepX) % 360f

        if (horizontalVelocity != 0f) {
            val rollStepZ = (horizontalVelocity * dt / radius) * (180f / PI.toFloat())
            rollAngleZ = (rollAngleZ - rollStepZ) % 360f
        }

        if (bounceAmp > 0f) {
            bouncePhase += dt * 6.5f
            position.y = radius + abs(sin(bouncePhase)) * bounceAmp
        }
    }
}

class PlayerCharacter {
    // Spatial coordinates: player moves in X and Y
    val position = Vector3(0f, 0f, 0f)

    // Physics parameters
    var forwardSpeed: Float = 10f
    val baseNormalSpeed: Float = 10f
    val minBrakedSpeed: Float = 3.2f
    val maxSprintSpeed: Float = 14.5f

    var horizontalVelocity: Float = 0f
    val maxHorizontalSpeed: Float = 8.5f
    val horizontalAcceleration: Float = 34f
    val horizontalDeceleration: Float = 28f

    var verticalVelocity: Float = 0f
    val jumpVelocity: Float = 9.0f
    val gravity: Float = 24.0f
    var isGrounded: Boolean = true

    // Realistic Procedural Animation & Articulation
    var runAnimationTime: Float = 0f
    var limbSwingAngle: Float = 0f // Main reference angle for stride
    var thighSwingLeft: Float = 0f
    var thighSwingRight: Float = 0f
    var kneeBendLeft: Float = 0f
    var kneeBendRight: Float = 0f
    var armSwingLeft: Float = 0f
    var armSwingRight: Float = 0f
    var elbowBendLeft: Float = 0f
    var elbowBendRight: Float = 0f
    var torsoTwist: Float = 0f
    var bodyBobOffset: Float = 0f
    var steerBankTilt: Float = 0f
    var headPitch: Float = 0f

    // Realistic Anatomical Proportions (Athletic cyber-runner ~1.95m)
    val headSize = 0.40f
    val torsoWidth = 0.58f
    val torsoHeight = 0.78f
    val torsoDepth = 0.32f
    val upperArmLen = 0.38f
    val foreArmLen = 0.36f
    val armThick = 0.16f
    val thighLen = 0.45f
    val shinLen = 0.46f
    val legThick = 0.18f

    // Total standing height for collision checks
    val totalHeight = 1.95f

    // Palette & Armor Layers
    var suitPrimaryColor = floatArrayOf(0.12f, 0.45f, 0.95f, 1f)
    var suitSecondaryColor = floatArrayOf(0.08f, 0.10f, 0.15f, 1f)
    var armorPlateColor = floatArrayOf(0.18f, 0.22f, 0.32f, 1f)
    var neonGlowColor = floatArrayOf(0.00f, 0.95f, 1.00f, 1f)
    var visorColor = floatArrayOf(0.00f, 0.95f, 1.00f, 1f)
    var skinColor = floatArrayOf(0.94f, 0.76f, 0.62f, 1f)
    var hairColor = floatArrayOf(0.15f, 0.12f, 0.10f, 1f)

    // Aliases for compatibility
    var shirtColor: FloatArray
        get() = suitPrimaryColor
        set(value) { suitPrimaryColor = value }
    var pantsColor: FloatArray
        get() = suitSecondaryColor
        set(value) { suitSecondaryColor = value }

    fun reset() {
        position.set(0f, 0f, 0f)
        forwardSpeed = baseNormalSpeed
        horizontalVelocity = 0f
        verticalVelocity = 0f
        isGrounded = true
        runAnimationTime = 0f
        limbSwingAngle = 0f
        thighSwingLeft = 0f
        thighSwingRight = 0f
        kneeBendLeft = 0f
        kneeBendRight = 0f
        armSwingLeft = 0f
        armSwingRight = 0f
        elbowBendLeft = 70f
        elbowBendRight = 70f
        torsoTwist = 0f
        bodyBobOffset = 0f
        steerBankTilt = 0f
        headPitch = 0f
    }

    fun applyColorPreset(presetName: String) {
        when (presetName) {
            "Classic Blue" -> {
                suitPrimaryColor = floatArrayOf(0.12f, 0.45f, 0.95f, 1f)
                suitSecondaryColor = floatArrayOf(0.08f, 0.10f, 0.15f, 1f)
                armorPlateColor = floatArrayOf(0.18f, 0.24f, 0.36f, 1f)
                neonGlowColor = floatArrayOf(0.00f, 0.95f, 1.00f, 1f)
                visorColor = floatArrayOf(0.00f, 0.95f, 1.00f, 1f)
            }
            "Neon Orange" -> {
                suitPrimaryColor = floatArrayOf(1.00f, 0.42f, 0.05f, 1f)
                suitSecondaryColor = floatArrayOf(0.12f, 0.12f, 0.14f, 1f)
                armorPlateColor = floatArrayOf(0.28f, 0.20f, 0.15f, 1f)
                neonGlowColor = floatArrayOf(1.00f, 0.85f, 0.10f, 1f)
                visorColor = floatArrayOf(1.00f, 0.88f, 0.20f, 1f)
            }
            "Emerald Runner" -> {
                suitPrimaryColor = floatArrayOf(0.05f, 0.78f, 0.35f, 1f)
                suitSecondaryColor = floatArrayOf(0.06f, 0.10f, 0.08f, 1f)
                armorPlateColor = floatArrayOf(0.14f, 0.28f, 0.18f, 1f)
                neonGlowColor = floatArrayOf(0.30f, 1.00f, 0.50f, 1f)
                visorColor = floatArrayOf(0.20f, 1.00f, 0.40f, 1f)
            }
            "Cyber Violet" -> {
                suitPrimaryColor = floatArrayOf(0.72f, 0.12f, 0.95f, 1f)
                suitSecondaryColor = floatArrayOf(0.12f, 0.06f, 0.16f, 1f)
                armorPlateColor = floatArrayOf(0.28f, 0.14f, 0.34f, 1f)
                neonGlowColor = floatArrayOf(1.00f, 0.20f, 0.90f, 1f)
                visorColor = floatArrayOf(0.95f, 0.15f, 0.95f, 1f)
            }
            "Solar Gold" -> {
                suitPrimaryColor = floatArrayOf(0.98f, 0.82f, 0.12f, 1f)
                suitSecondaryColor = floatArrayOf(0.16f, 0.14f, 0.10f, 1f)
                armorPlateColor = floatArrayOf(0.35f, 0.30f, 0.18f, 1f)
                neonGlowColor = floatArrayOf(1.00f, 0.95f, 0.40f, 1f)
                visorColor = floatArrayOf(1.00f, 0.92f, 0.30f, 1f)
            }
        }
    }

    fun jump(): Boolean {
        if (isGrounded) {
            verticalVelocity = jumpVelocity
            isGrounded = false
            return true
        }
        return false
    }

    fun update(
        dt: Float,
        leftHeld: Boolean,
        rightHeld: Boolean,
        brakeHeld: Boolean,
        roadHalfWidth: Float
    ) {
        // Forward speed adjustment based on brake input
        val targetForwardSpeed = if (brakeHeld) minBrakedSpeed else baseNormalSpeed
        val accelRate = if (targetForwardSpeed > forwardSpeed) 8.5f else 16.0f
        forwardSpeed += (targetForwardSpeed - forwardSpeed) * (accelRate * dt).coerceAtMost(1f)

        // Horizontal velocity calculation with momentum and analog steering feel
        val targetInput = when {
            leftHeld && !rightHeld -> -1f
            rightHeld && !leftHeld -> 1f
            else -> 0f
        }

        if (targetInput != 0f) {
            val targetVel = targetInput * maxHorizontalSpeed
            if (targetInput > 0f) {
                horizontalVelocity = (horizontalVelocity + horizontalAcceleration * dt).coerceAtMost(targetVel)
            } else {
                horizontalVelocity = (horizontalVelocity - horizontalAcceleration * dt).coerceAtLeast(targetVel)
            }
        } else {
            if (horizontalVelocity > 0f) {
                horizontalVelocity = (horizontalVelocity - horizontalDeceleration * dt).coerceAtLeast(0f)
            } else if (horizontalVelocity < 0f) {
                horizontalVelocity = (horizontalVelocity + horizontalDeceleration * dt).coerceAtMost(0f)
            }
        }

        position.x += horizontalVelocity * dt

        val playableMargin = roadHalfWidth - 0.7f
        if (position.x > playableMargin) {
            position.x = playableMargin
            horizontalVelocity = 0f
        } else if (position.x < -playableMargin) {
            position.x = -playableMargin
            horizontalVelocity = 0f
        }

        if (!isGrounded) {
            verticalVelocity -= gravity * dt
            position.y += verticalVelocity * dt
            if (position.y <= 0f) {
                position.y = 0f
                verticalVelocity = 0f
                isGrounded = true
            }
        }

        // Realistic Procedural Running Biomechanics
        val strideRate = (forwardSpeed / baseNormalSpeed) * 12.0f
        runAnimationTime += dt * strideRate
        val strideSin = sin(runAnimationTime)
        limbSwingAngle = strideSin * 38f

        // Natural hip and thigh swinging
        thighSwingLeft = strideSin * 36f
        thighSwingRight = -thighSwingLeft

        // Dynamic Knee Flexion: knee bends as thigh swings back into trailing step
        kneeBendLeft = if (thighSwingLeft > 0f) (thighSwingLeft * 1.55f).coerceAtMost(70f) else 4f
        kneeBendRight = if (thighSwingRight > 0f) (thighSwingRight * 1.55f).coerceAtMost(70f) else 4f

        // Arm swing counter-balances legs: left arm moves forward with right leg
        armSwingLeft = -thighSwingLeft * 0.85f
        armSwingRight = -armSwingLeft

        // Forearms stay bent in high-performance runner posture (~75-85 deg)
        elbowBendLeft = 76f + strideSin * 10f
        elbowBendRight = 76f - strideSin * 10f

        // Subtle torso counter-twist
        torsoTwist = -strideSin * 7f

        // Aerodynamic forward lean scales with speed
        headPitch = 6f + (forwardSpeed / baseNormalSpeed) * 5f

        // Vertical bobbing
        bodyBobOffset = if (isGrounded) {
            abs(sin(runAnimationTime * 2f)) * 0.08f
        } else {
            0.05f
        }

        // Banking lean into lateral turns
        val targetTilt = (-horizontalVelocity / maxHorizontalSpeed) * 16f
        steerBankTilt += (targetTilt - steerBankTilt) * (14f * dt).coerceAtMost(1f)
    }
}

class RoadSegment(val id: Int, var zStart: Float, val length: Float = 30f) {
    // Road surface extends from zStart to zStart - length
    // Center dashed markings, curbs, roadside scenery
}

data class SceneryItem(
    val x: Float,
    val z: Float,
    val type: Int, // 0 = Pine Tree, 1 = Round Tree, 2 = Rock Boulder
    val scale: Float,
    val rotationY: Float
)

class Particle(
    val position: Vector3 = Vector3(),
    val velocity: Vector3 = Vector3(),
    var lifetime: Float = 0f,
    var maxLife: Float = 1f,
    var size: Float = 0.2f,
    val color: FloatArray = floatArrayOf(1f, 0.8f, 0.2f, 1f)
)

class ParticleSystem(val maxParticles: Int = 80) {
    val particles = Array(maxParticles) { Particle() }

    fun emitBurst(origin: Vector3, count: Int, baseColor: FloatArray) {
        var emitted = 0
        for (p in particles) {
            if (p.lifetime <= 0f) {
                p.position.set(origin.x, origin.y, origin.z)
                val vx = (kotlin.random.Random.nextFloat() - 0.5f) * 12f
                val vy = kotlin.random.Random.nextFloat() * 10f + 2f
                val vz = (kotlin.random.Random.nextFloat() - 0.5f) * 12f
                p.velocity.set(vx, vy, vz)
                p.lifetime = 0.6f + kotlin.random.Random.nextFloat() * 0.4f
                p.maxLife = p.lifetime
                p.size = 0.16f + kotlin.random.Random.nextFloat() * 0.15f
                p.color[0] = baseColor[0]
                p.color[1] = baseColor[1]
                p.color[2] = baseColor[2]
                p.color[3] = 1f
                emitted++
                if (emitted >= count) break
            }
        }
    }

    fun update(dt: Float) {
        for (p in particles) {
            if (p.lifetime > 0f) {
                p.lifetime -= dt
                p.position.x += p.velocity.x * dt
                p.position.y += p.velocity.y * dt
                p.position.z += p.velocity.z * dt
                p.velocity.y -= 20f * dt // Gravity
                p.color[3] = (p.lifetime / p.maxLife).coerceIn(0f, 1f)
            }
        }
    }
}

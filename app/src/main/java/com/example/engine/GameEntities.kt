package com.example.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

data class SectorInfo(
    val id: Int,
    val name: String,
    val subtitle: String,
    val minDistance: Float,
    val speedMultiplier: Float,
    val fogColor: FloatArray,
    val fogHorizonColor: FloatArray,
    val ambientColor: FloatArray,
    val lightColor: FloatArray
)

class SpeedPad(val id: Int) {
    var isActive: Boolean = false
    val position = Vector3()
    var glowPulse: Float = 0f
    val width: Float = 3.0f
    val length: Float = 4.2f

    fun reset(x: Float, z: Float) {
        position.set(x, 0.04f, z)
        isActive = true
        glowPulse = 0f
    }

    fun update(dt: Float) {
        if (!isActive) return
        glowPulse = (glowPulse + dt * 4.5f) % (2f * PI.toFloat())
    }
}

enum class CollectibleType(
    val displayName: String,
    val description: String,
    val duration: Float,
    val color: FloatArray
) {
    SHIELD(
        "ENERGY SHIELD",
        "Absorbs 1 boulder collision and saves your run!",
        14.0f,
        floatArrayOf(0.0f, 0.95f, 1.0f, 1.0f)
    ),
    SPEED_BOOST(
        "HYPER BOOST",
        "Surge forward with intense rocket propulsion!",
        6.0f,
        floatArrayOf(0.0f, 1.0f, 0.55f, 1.0f)
    ),
    SCORE_MULTIPLIER(
        "2X MULTIPLIER",
        "Doubles all distance and dodge points gained!",
        9.0f,
        floatArrayOf(1.0f, 0.85f, 0.12f, 1.0f)
    ),
    ENERGY_CELL(
        "ENERGY CELL",
        "Instant +150 bonus points and combo refill!",
        0.0f,
        floatArrayOf(1.0f, 0.25f, 0.85f, 1.0f)
    )
}

class CollectibleItem(val id: Int) {
    var isActive: Boolean = false
    val position = Vector3()
    var type: CollectibleType = CollectibleType.SHIELD
    var rotationY: Float = 0f
    var bobPhase: Float = 0f
    var glowPulse: Float = 0f
    val radius: Float = 0.65f

    fun reset(collectibleType: CollectibleType, x: Float, z: Float) {
        type = collectibleType
        position.set(x, 0.85f, z)
        isActive = true
        rotationY = kotlin.random.Random.nextFloat() * 360f
        bobPhase = kotlin.random.Random.nextFloat() * 2f * PI.toFloat()
        glowPulse = 0f
    }

    fun update(dt: Float) {
        if (!isActive) return
        rotationY = (rotationY + dt * 110f) % 360f
        bobPhase = (bobPhase + dt * 3.2f) % (2f * PI.toFloat())
        glowPulse = (glowPulse + dt * 4.5f) % (2f * PI.toFloat())
        position.y = 0.85f + sin(bobPhase) * 0.18f
    }
}

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

    var trailDustTimer: Float = 0f
    var wasInAir: Boolean = false
    var curbCooldownTimer: Float = 0f

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
        bounceAmp = if (type == BallType.BOUNCING) 0.65f else 0f
        hasDodged = false
        trailDustTimer = 0.06f
        wasInAir = false
        curbCooldownTimer = 0f

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

enum class CharacterModelId(
    val id: String,
    val displayName: String,
    val title: String,
    val price: Int,
    val description: String,
    val perkName: String,
    val perkDescription: String,
    val armorRating: Int, // 1 to 5
    val speedRating: Int, // 1 to 5
    val techRating: Int // 1 to 5
) {
    VANGUARD(
        id = "vanguard",
        displayName = "Vanguard Striker",
        title = "Cyber Infiltrator",
        price = 0,
        description = "High-agility cybernetic runner equipped with twin high-compression jet thrusters and kinetic gauntlets.",
        perkName = "Agile Core",
        perkDescription = "Balanced handling & rapid kinetic recovery.",
        armorRating = 3,
        speedRating = 4,
        techRating = 3
    ),
    TITAN(
        id = "titan",
        displayName = "Titan Mech",
        title = "Heavy Armored Exo-Suit",
        price = 1200,
        description = "Massive reinforced hydraulic mech chassis with quad cooling turbines and shock-absorbing greaves.",
        perkName = "Juggernaut Plating",
        perkDescription = "+30% Shield duration (18s) & +0.5s recovery grace on collision.",
        armorRating = 5,
        speedRating = 2,
        techRating = 4
    ),
    VALKYRIE(
        id = "valkyrie",
        displayName = "Valkyrie Aero",
        title = "Supersonic Glider",
        price = 2500,
        description = "Aerodynamic carbon chassis featuring dual swept kinetic glider wings and micro-afterburners.",
        perkName = "Supersonic Lift",
        perkDescription = "+25% Speed Boost duration and +15% sprint velocity.",
        armorRating = 2,
        speedRating = 5,
        techRating = 4
    ),
    PHANTOM(
        id = "phantom",
        displayName = "Phantom Shinobi",
        title = "Shadow Assassin",
        price = 4200,
        description = "Stealth operative with twin back-mounted cyber katanas, dynamic flowing kinetic wind scarf, and razor cowl.",
        perkName = "Shadow Reflexes",
        perkDescription = "+50% Near-Miss dodge score & extended combo window.",
        armorRating = 3,
        speedRating = 4,
        techRating = 5
    ),
    CHRONOS(
        id = "chronos",
        displayName = "Chronos Prime",
        title = "Celestial Sovereign",
        price = 7000,
        description = "Transcendent golden deity crowned with a rotating celestial halo and floating energy crystal core.",
        perkName = "Midas Harvest",
        perkDescription = "+25% Bonus Points on all dodges & Energy Cells.",
        armorRating = 4,
        speedRating = 4,
        techRating = 5
    );

    companion object {
        fun fromId(id: String): CharacterModelId {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: VANGUARD
        }
    }
}

class PlayerCharacter {
    // Spatial coordinates: player moves in X and Y
    val position = Vector3(0f, 0f, 0f)

    // Current 3D Character Model Variant
    var model: CharacterModelId = CharacterModelId.VANGUARD

    // Physics parameters
    var forwardSpeed: Float = 10f
    var baseNormalSpeed: Float = 10f
    val minBrakedSpeed: Float = 3.2f
    var maxSprintSpeed: Float = 14.5f

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

    // Realistic Anatomical Proportions (Dynamic per character model)
    var headSize: Float = 0.40f
    var torsoWidth: Float = 0.58f
    var torsoHeight: Float = 0.78f
    var torsoDepth: Float = 0.32f
    var upperArmLen: Float = 0.38f
    var foreArmLen: Float = 0.36f
    var armThick: Float = 0.16f
    var thighLen: Float = 0.45f
    var shinLen: Float = 0.46f
    var legThick: Float = 0.18f

    // Total standing height for collision checks
    var totalHeight: Float = 1.95f

    // Dynamic Physics & State FX
    var isTumbling: Boolean = false
    var tumblePitch: Float = 0f
    var tumbleRoll: Float = 0f
    var tumbleYaw: Float = 0f
    var tumbleVelY: Float = 0f
    var boostTimer: Float = 0f
    val isBoosting: Boolean get() = boostTimer > 0f
    var landingSquash: Float = 0f
    var nearMissTilt: Float = 0f

    // Power-Up State Systems
    var hasShield: Boolean = false
    var shieldTimer: Float = 0f
    var maxShieldDuration: Float = 14f
    val isShieldActive: Boolean get() = hasShield && shieldTimer > 0f

    var invincibleGraceTimer: Float = 0f
    val isInvincible: Boolean get() = isShieldActive || invincibleGraceTimer > 0f

    var scoreMultiplierTimer: Float = 0f
    var maxMultiplierDuration: Float = 9f
    var scoreMultiplierValue: Int = 1
    val isScoreBoosted: Boolean get() = scoreMultiplierTimer > 0f && scoreMultiplierValue > 1

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
        isTumbling = false
        tumblePitch = 0f
        tumbleRoll = 0f
        tumbleYaw = 0f
        tumbleVelY = 0f
        boostTimer = 0f
        hasShield = false
        shieldTimer = 0f
        invincibleGraceTimer = 0f
        scoreMultiplierTimer = 0f
        scoreMultiplierValue = 1
        landingSquash = 0f
        nearMissTilt = 0f
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

    fun triggerTumble() {
        isTumbling = true
        tumbleVelY = 6.5f
    }

    fun applySpeedBoost(duration: Float = 2.5f) {
        val boostDur = if (model == CharacterModelId.VALKYRIE) duration * 1.25f else duration
        boostTimer = boostDur
    }

    fun activateShield(duration: Float = 14f) {
        val dur = if (model == CharacterModelId.TITAN) 18f else duration
        hasShield = true
        shieldTimer = dur
        maxShieldDuration = dur
    }

    fun breakShield(gracePeriod: Float = 1.4f) {
        hasShield = false
        shieldTimer = 0f
        val recoveryGrace = if (model == CharacterModelId.TITAN) gracePeriod + 0.5f else gracePeriod
        invincibleGraceTimer = recoveryGrace
    }

    fun activateScoreMultiplier(duration: Float = 9f, multiplier: Int = 2) {
        scoreMultiplierValue = multiplier
        scoreMultiplierTimer = duration
        maxMultiplierDuration = duration
    }

    fun applyCharacterModel(newModel: CharacterModelId) {
        model = newModel
        when (newModel) {
            CharacterModelId.VANGUARD -> {
                // Agile Cyber Striker proportions & colors
                torsoWidth = 0.54f
                torsoHeight = 0.78f
                torsoDepth = 0.32f
                legThick = 0.18f
                armThick = 0.16f
                headSize = 0.38f
                baseNormalSpeed = 10.0f
                maxSprintSpeed = 14.5f

                suitPrimaryColor = floatArrayOf(0.12f, 0.45f, 0.95f, 1f) // Cobalt Blue
                suitSecondaryColor = floatArrayOf(0.08f, 0.10f, 0.15f, 1f) // Dark Carbon
                armorPlateColor = floatArrayOf(0.18f, 0.24f, 0.36f, 1f) // Tactical Navy
                neonGlowColor = floatArrayOf(0.00f, 0.95f, 1.00f, 1f) // Cyan Laser
                visorColor = floatArrayOf(0.00f, 0.95f, 1.00f, 1f)
            }
            CharacterModelId.TITAN -> {
                // Massive Heavy Armored Mech proportions & colors
                torsoWidth = 0.72f // 33% broader torso!
                torsoHeight = 0.84f
                torsoDepth = 0.42f // Heavy reinforced chest depth
                legThick = 0.24f // Heavy hydraulic legs
                armThick = 0.22f // Bulky mech arms
                headSize = 0.44f // Enclosed combat mech helmet
                baseNormalSpeed = 9.8f
                maxSprintSpeed = 14.0f

                suitPrimaryColor = floatArrayOf(0.95f, 0.42f, 0.05f, 1f) // Hazard Industrial Orange
                suitSecondaryColor = floatArrayOf(0.12f, 0.13f, 0.16f, 1f) // Heavy Gunmetal Grey
                armorPlateColor = floatArrayOf(0.25f, 0.26f, 0.30f, 1f) // Reinforced Titanium
                neonGlowColor = floatArrayOf(1.00f, 0.75f, 0.00f, 1f) // Amber Reactor Core
                visorColor = floatArrayOf(1.00f, 0.30f, 0.00f, 1f) // Menacing Dual Optics
            }
            CharacterModelId.VALKYRIE -> {
                // Aerodynamic Winged Glider proportions & colors
                torsoWidth = 0.48f // Sleek low-drag fuselage
                torsoHeight = 0.76f
                torsoDepth = 0.28f
                legThick = 0.16f
                armThick = 0.14f
                headSize = 0.36f
                baseNormalSpeed = 10.6f // Higher baseline cruise speed
                maxSprintSpeed = 15.5f

                suitPrimaryColor = floatArrayOf(0.05f, 0.85f, 0.42f, 1f) // Emerald Kinetic Green
                suitSecondaryColor = floatArrayOf(0.06f, 0.12f, 0.10f, 1f) // Carbon Fiber
                armorPlateColor = floatArrayOf(0.12f, 0.32f, 0.22f, 1f) // Aero Alloy
                neonGlowColor = floatArrayOf(0.35f, 1.00f, 0.65f, 1f) // High-Voltage Lime
                visorColor = floatArrayOf(0.20f, 1.00f, 0.50f, 1f)
            }
            CharacterModelId.PHANTOM -> {
                // Stealth Shinobi Assassin proportions & colors
                torsoWidth = 0.50f
                torsoHeight = 0.78f
                torsoDepth = 0.30f
                legThick = 0.17f
                armThick = 0.15f
                headSize = 0.38f
                baseNormalSpeed = 10.2f
                maxSprintSpeed = 15.0f

                suitPrimaryColor = floatArrayOf(0.70f, 0.15f, 0.95f, 1f) // Cyber Shadow Violet
                suitSecondaryColor = floatArrayOf(0.08f, 0.06f, 0.12f, 1f) // Midnight Obsidian
                armorPlateColor = floatArrayOf(0.24f, 0.14f, 0.30f, 1f) // Shadow Steel
                neonGlowColor = floatArrayOf(1.00f, 0.15f, 0.85f, 1f) // Magenta Ninja Energy
                visorColor = floatArrayOf(1.00f, 0.10f, 0.90f, 1f)
            }
            CharacterModelId.CHRONOS -> {
                // Transcendent Celestial Sovereign proportions & colors
                torsoWidth = 0.58f
                torsoHeight = 0.80f
                torsoDepth = 0.34f
                legThick = 0.19f
                armThick = 0.17f
                headSize = 0.40f
                baseNormalSpeed = 10.0f
                maxSprintSpeed = 14.8f

                suitPrimaryColor = floatArrayOf(1.00f, 0.86f, 0.18f, 1f) // Imperial Solar Gold
                suitSecondaryColor = floatArrayOf(0.90f, 0.92f, 0.96f, 1f) // Royal Marble White
                armorPlateColor = floatArrayOf(0.32f, 0.26f, 0.14f, 1f) // Sun-Forged Bronze
                neonGlowColor = floatArrayOf(1.00f, 0.95f, 0.45f, 1f) // Solar Flare Halo
                visorColor = floatArrayOf(1.00f, 0.95f, 0.35f, 1f)
            }
        }
    }

    fun applyColorPreset(presetName: String) {
        val mapped = when (presetName) {
            "Classic Blue", "vanguard" -> CharacterModelId.VANGUARD
            "Neon Orange", "titan" -> CharacterModelId.TITAN
            "Emerald Runner", "valkyrie" -> CharacterModelId.VALKYRIE
            "Cyber Violet", "phantom" -> CharacterModelId.PHANTOM
            "Solar Gold", "chronos" -> CharacterModelId.CHRONOS
            else -> CharacterModelId.fromId(presetName)
        }
        applyCharacterModel(mapped)
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
        if (isTumbling) {
            tumblePitch += 380f * dt
            tumbleRoll += 220f * dt
            tumbleYaw += 150f * dt
            tumbleVelY -= gravity * 0.8f * dt
            position.y = (position.y + tumbleVelY * dt).coerceAtLeast(0.15f)
            return
        }

        if (boostTimer > 0f) {
            boostTimer = (boostTimer - dt).coerceAtLeast(0f)
        }

        if (shieldTimer > 0f) {
            shieldTimer = (shieldTimer - dt).coerceAtLeast(0f)
            if (shieldTimer <= 0f) {
                hasShield = false
            }
        }

        if (invincibleGraceTimer > 0f) {
            invincibleGraceTimer = (invincibleGraceTimer - dt).coerceAtLeast(0f)
        }

        if (scoreMultiplierTimer > 0f) {
            scoreMultiplierTimer = (scoreMultiplierTimer - dt).coerceAtLeast(0f)
            if (scoreMultiplierTimer <= 0f) {
                scoreMultiplierValue = 1
            }
        }

        // Forward speed adjustment based on brake input & turbo boost
        val boostBonus = if (isBoosting) 5.5f else 0f
        val targetForwardSpeed = if (brakeHeld) minBrakedSpeed else (baseNormalSpeed + boostBonus)
        val accelRate = if (targetForwardSpeed > forwardSpeed) 10.5f else 16.0f
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
                landingSquash = 0.20f // Cushion impact spring
            }
        }

        if (landingSquash > 0f) {
            landingSquash = (landingSquash - dt * 1.5f).coerceAtLeast(0f)
        }

        if (nearMissTilt != 0f) {
            nearMissTilt += (0f - nearMissTilt) * (8f * dt).coerceAtMost(1f)
        }

        // Realistic Procedural Running Biomechanics
        val strideRate = (forwardSpeed / baseNormalSpeed) * 12.0f
        runAnimationTime += dt * strideRate
        val strideSin = sin(runAnimationTime)

        if (isGrounded) {
            limbSwingAngle = strideSin * 38f
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

            torsoTwist = -strideSin * 7f
            bodyBobOffset = (abs(sin(runAnimationTime * 2f)) * 0.08f) - landingSquash * 0.35f
        } else {
            // Mid-air Jump Pose (Athletic hurdle tuck & reach)
            thighSwingLeft = 35f
            thighSwingRight = -25f
            kneeBendLeft = 55f
            kneeBendRight = 45f
            armSwingLeft = -40f
            armSwingRight = -40f
            elbowBendLeft = 90f
            elbowBendRight = 90f
            torsoTwist = 0f
            bodyBobOffset = 0.05f
        }

        // Aerodynamic forward lean scales with speed
        headPitch = 6f + (forwardSpeed / baseNormalSpeed) * 6f

        // Banking lean into lateral turns + near-miss dynamic dodge evasion
        val targetTilt = (-horizontalVelocity / maxHorizontalSpeed) * 16f + nearMissTilt
        steerBankTilt += (targetTilt - steerBankTilt) * (14f * dt).coerceAtMost(1f)
    }
}

class RoadSegment(val id: Int, var zStart: Float, val length: Float = 30f) {
    // Road surface extends from zStart to zStart - length
}

data class SceneryItem(
    val x: Float,
    val z: Float,
    val type: Int, // 0 = Pine Tree, 1 = Cyber Light Tower, 2 = Boulder, 3 = Floating Crystal, 4 = SciFi Pyramid
    val scale: Float,
    val rotationY: Float
)

enum class ParticleType {
    SPARK,
    DUST_CLOUD,
    ROCK_DEBRIS,
    SHOCKWAVE,
    SPEED_STREAK
}

class Particle(
    val position: Vector3 = Vector3(),
    val velocity: Vector3 = Vector3(),
    var lifetime: Float = 0f,
    var maxLife: Float = 1f,
    var size: Float = 0.2f,
    var growthRate: Float = 0f,
    var rotation: Float = 0f,
    var rotSpeed: Float = 0f,
    var bounciness: Float = 0.4f,
    var particleType: ParticleType = ParticleType.SPARK,
    val color: FloatArray = floatArrayOf(1f, 0.8f, 0.2f, 1f)
)

class ParticleSystem(val maxParticles: Int = 320) {
    val particles = Array(maxParticles) { Particle() }

    fun emitBurst(origin: Vector3, count: Int, baseColor: FloatArray) {
        var emitted = 0
        for (p in particles) {
            if (p.lifetime <= 0f) {
                p.particleType = ParticleType.SPARK
                p.position.set(origin.x, origin.y, origin.z)
                val vx = (kotlin.random.Random.nextFloat() - 0.5f) * 14f
                val vy = kotlin.random.Random.nextFloat() * 11f + 2.5f
                val vz = (kotlin.random.Random.nextFloat() - 0.5f) * 14f
                p.velocity.set(vx, vy, vz)
                p.lifetime = 0.6f + kotlin.random.Random.nextFloat() * 0.4f
                p.maxLife = p.lifetime
                p.size = 0.18f + kotlin.random.Random.nextFloat() * 0.16f
                p.growthRate = 0f
                p.rotation = kotlin.random.Random.nextFloat() * 360f
                p.rotSpeed = (kotlin.random.Random.nextFloat() - 0.5f) * 200f
                p.color[0] = baseColor[0]
                p.color[1] = baseColor[1]
                p.color[2] = baseColor[2]
                p.color[3] = 1f
                emitted++
                if (emitted >= count) break
            }
        }
    }

    /**
     * Emits continuous billowing rolling dust clouds behind a moving boulder.
     */
    fun emitBoulderTrailDust(origin: Vector3, radius: Float, ballSpeed: Float, baseColor: FloatArray) {
        var emitted = 0
        val count = 2
        for (p in particles) {
            if (p.lifetime <= 0f) {
                p.particleType = ParticleType.DUST_CLOUD
                // Emit at ground contact patch directly behind the rolling ball
                val offsetX = (kotlin.random.Random.nextFloat() - 0.5f) * (radius * 0.85f)
                val offsetZ = -radius * 0.65f + (kotlin.random.Random.nextFloat() - 0.5f) * 0.25f
                p.position.set(origin.x + offsetX, 0.07f, origin.z + offsetZ)

                // Swirling billowing velocity (hugs the ground, spreads laterally)
                val vx = (kotlin.random.Random.nextFloat() - 0.5f) * 2.2f
                val vy = kotlin.random.Random.nextFloat() * 0.18f + 0.04f // Low, ground-hugging
                val vz = -kotlin.random.Random.nextFloat() * 1.4f - 0.4f // Gentle drift behind ball
                p.velocity.set(vx, vy, vz)

                p.lifetime = 0.52f + kotlin.random.Random.nextFloat() * 0.22f
                p.maxLife = p.lifetime
                // Moderately sized road dust: clearly visible, but ground-hugging so it never hides incoming balls
                p.size = radius * (0.42f + kotlin.random.Random.nextFloat() * 0.12f)
                p.growthRate = 0.28f + kotlin.random.Random.nextFloat() * 0.12f
                p.rotation = kotlin.random.Random.nextFloat() * 360f
                p.rotSpeed = (kotlin.random.Random.nextFloat() - 0.5f) * 60f

                // Warm asphalt road dust with soft transparency
                val tint = 0.08f
                p.color[0] = 0.88f * (1f - tint) + baseColor[0] * tint
                p.color[1] = 0.84f * (1f - tint) + baseColor[1] * tint
                p.color[2] = 0.78f * (1f - tint) + baseColor[2] * tint
                p.color[3] = 0.32f // Clearly visible initial translucency, delicate so it never hides obstacles

                emitted++
                if (emitted >= count) break
            }
        }

        // Occasionally kick up a small tumbling rock gravel chip
        if (kotlin.random.Random.nextFloat() < 0.20f) {
            for (p in particles) {
                if (p.lifetime <= 0f) {
                    p.particleType = ParticleType.ROCK_DEBRIS
                    p.position.set(origin.x + (kotlin.random.Random.nextFloat() - 0.5f) * radius * 0.7f, 0.10f, origin.z - radius * 0.4f)
                    p.velocity.set(
                        (kotlin.random.Random.nextFloat() - 0.5f) * 3.5f,
                        kotlin.random.Random.nextFloat() * 3.0f + 0.8f,
                        kotlin.random.Random.nextFloat() * 3.5f + 1.2f
                    )
                    p.lifetime = 0.6f + kotlin.random.Random.nextFloat() * 0.25f
                    p.maxLife = p.lifetime
                    p.size = 0.08f + kotlin.random.Random.nextFloat() * 0.06f
                    p.growthRate = 0f
                    p.rotation = kotlin.random.Random.nextFloat() * 360f
                    p.rotSpeed = (kotlin.random.Random.nextFloat() - 0.5f) * 350f
                    p.bounciness = 0.45f
                    p.color[0] = 0.45f; p.color[1] = 0.42f; p.color[2] = 0.40f; p.color[3] = 0.85f
                    break
                }
            }
        }
    }

    /**
     * Heavy ground collision shockwave, rock shatter, and explosive dust when a boulder slams the ground.
     */
    fun emitBoulderImpact(origin: Vector3, radius: Float, hitColor: FloatArray) {
        // 1. Expanding Ground Shockwave Ring
        emitShockwave(Vector3(origin.x, 0.04f, origin.z), 16, floatArrayOf(0.95f, 0.90f, 0.80f, 0.70f))

        // 2. High-speed rock fragment spray
        var debrisEmitted = 0
        for (p in particles) {
            if (p.lifetime <= 0f) {
                p.particleType = ParticleType.ROCK_DEBRIS
                p.position.set(origin.x, 0.12f, origin.z)
                val angle = kotlin.random.Random.nextFloat() * (2f * PI.toFloat())
                val speed = kotlin.random.Random.nextFloat() * 7f + 2f
                p.velocity.set(
                    kotlin.math.cos(angle) * speed,
                    kotlin.random.Random.nextFloat() * 6f + 1.8f,
                    kotlin.math.sin(angle) * speed
                )
                p.lifetime = 0.60f + kotlin.random.Random.nextFloat() * 0.3f
                p.maxLife = p.lifetime
                p.size = 0.10f + kotlin.random.Random.nextFloat() * 0.08f
                p.growthRate = 0f
                p.rotation = kotlin.random.Random.nextFloat() * 360f
                p.rotSpeed = (kotlin.random.Random.nextFloat() - 0.5f) * 450f
                p.bounciness = 0.50f
                p.color[0] = hitColor[0] * 0.7f + 0.2f
                p.color[1] = hitColor[1] * 0.7f + 0.2f
                p.color[2] = hitColor[2] * 0.7f + 0.2f
                p.color[3] = 0.80f

                debrisEmitted++
                if (debrisEmitted >= 8) break
            }
        }

        // 3. Billowing mushrooming dust puffs (ground-hugging, clear visibility, never hides upcoming balls)
        var dustEmitted = 0
        for (p in particles) {
            if (p.lifetime <= 0f) {
                p.particleType = ParticleType.DUST_CLOUD
                p.position.set(
                    origin.x + (kotlin.random.Random.nextFloat() - 0.5f) * radius * 0.8f,
                    0.08f,
                    origin.z + (kotlin.random.Random.nextFloat() - 0.5f) * radius * 0.8f
                )
                p.velocity.set(
                    (kotlin.random.Random.nextFloat() - 0.5f) * 3.2f,
                    kotlin.random.Random.nextFloat() * 0.22f + 0.04f,
                    (kotlin.random.Random.nextFloat() - 0.5f) * 3.2f
                )
                p.lifetime = 0.58f + kotlin.random.Random.nextFloat() * 0.22f
                p.maxLife = p.lifetime
                p.size = radius * 0.40f
                p.growthRate = 0.36f
                p.rotation = kotlin.random.Random.nextFloat() * 360f
                p.rotSpeed = (kotlin.random.Random.nextFloat() - 0.5f) * 90f
                p.color[0] = 0.88f; p.color[1] = 0.84f; p.color[2] = 0.80f; p.color[3] = 0.40f

                dustEmitted++
                if (dustEmitted >= 6) break
            }
        }
    }

    /**
     * Emits high-energy sparks and pulverized barrier debris when crossing boulders scrape the curbs.
     */
    fun emitCurbCollision(origin: Vector3, isLeftSide: Boolean, ballColor: FloatArray) {
        val inwardsDir = if (isLeftSide) 1f else -1f
        var debrisCount = 0
        var sparkCount = 0

        // Emit dedicated rock debris
        for (p in particles) {
            if (p.lifetime <= 0f) {
                p.particleType = ParticleType.ROCK_DEBRIS
                p.position.set(origin.x, 0.35f + kotlin.random.Random.nextFloat() * 0.4f, origin.z)
                val vx = (kotlin.random.Random.nextFloat() * 8f + 2.5f) * inwardsDir
                val vy = kotlin.random.Random.nextFloat() * 6f + 2.0f
                val vz = (kotlin.random.Random.nextFloat() - 0.5f) * 6f + 2f
                p.velocity.set(vx, vy, vz)
                p.lifetime = 0.70f + kotlin.random.Random.nextFloat() * 0.3f
                p.maxLife = p.lifetime
                p.size = 0.16f + kotlin.random.Random.nextFloat() * 0.10f
                p.growthRate = 0f
                p.rotation = kotlin.random.Random.nextFloat() * 360f
                p.rotSpeed = (kotlin.random.Random.nextFloat() - 0.5f) * 600f
                p.bounciness = 0.50f
                p.color[0] = 0.85f; p.color[1] = 0.28f; p.color[2] = 0.22f; p.color[3] = 1f
                debrisCount++
                if (debrisCount >= 6) break
            }
        }

        // Emit dedicated sparks
        for (p in particles) {
            if (p.lifetime <= 0f) {
                p.particleType = ParticleType.SPARK
                p.position.set(origin.x, 0.30f + kotlin.random.Random.nextFloat() * 0.3f, origin.z)
                val vx = (kotlin.random.Random.nextFloat() * 10f + 4f) * inwardsDir
                val vy = kotlin.random.Random.nextFloat() * 7f + 1.5f
                val vz = (kotlin.random.Random.nextFloat() - 0.5f) * 6f
                p.velocity.set(vx, vy, vz)
                p.lifetime = 0.35f
                p.maxLife = 0.35f
                p.size = 0.12f
                p.growthRate = 0f
                p.color[0] = 1.0f; p.color[1] = 0.85f; p.color[2] = 0.20f; p.color[3] = 1f
                sparkCount++
                if (sparkCount >= 8) break
            }
        }

        // Billowing barrier smoke
        for (p in particles) {
            if (p.lifetime <= 0f) {
                p.particleType = ParticleType.DUST_CLOUD
                p.position.set(origin.x, 0.20f, origin.z)
                p.velocity.set(inwardsDir * 1.8f, 1.0f, 1.0f)
                p.lifetime = 0.50f
                p.maxLife = 0.50f
                p.size = 0.22f
                p.growthRate = 0.30f
                p.color[0] = 0.75f; p.color[1] = 0.75f; p.color[2] = 0.80f; p.color[3] = 0.25f
                break
            }
        }
    }

    /**
     * Explosive debris shatter when a boulder collides with roadside scenery (rocks, pylons, trees).
     */
    fun emitSceneryShatter(origin: Vector3, sceneryType: Int, ballColor: FloatArray) {
        var count = 0
        for (p in particles) {
            if (p.lifetime <= 0f) {
                p.particleType = ParticleType.ROCK_DEBRIS
                p.position.set(origin.x, 0.8f + kotlin.random.Random.nextFloat() * 0.8f, origin.z)
                val angle = kotlin.random.Random.nextFloat() * (2f * PI.toFloat())
                val speed = kotlin.random.Random.nextFloat() * 12f + 4f
                p.velocity.set(
                    kotlin.math.cos(angle) * speed,
                    kotlin.random.Random.nextFloat() * 10f + 2f,
                    kotlin.math.sin(angle) * speed
                )
                p.lifetime = 0.85f + kotlin.random.Random.nextFloat() * 0.4f
                p.maxLife = p.lifetime
                p.size = 0.20f + kotlin.random.Random.nextFloat() * 0.16f
                p.growthRate = 0f
                p.rotation = kotlin.random.Random.nextFloat() * 360f
                p.rotSpeed = (kotlin.random.Random.nextFloat() - 0.5f) * 600f
                p.bounciness = 0.40f

                when (sceneryType) {
                    0 -> { // Pine Tree wood / needles
                        p.color[0] = 0.18f; p.color[1] = 0.45f; p.color[2] = 0.22f; p.color[3] = 1f
                    }
                    1 -> { // Cyber Pylon sparks & metal
                        p.color[0] = 0.0f; p.color[1] = 0.95f; p.color[2] = 1.0f; p.color[3] = 1f
                    }
                    else -> { // Rock boulder shards
                        p.color[0] = 0.52f; p.color[1] = 0.50f; p.color[2] = 0.48f; p.color[3] = 1f
                    }
                }

                count++
                if (count >= 16) break
            }
        }
    }

    fun emitDust(origin: Vector3, count: Int) {
        var emitted = 0
        for (p in particles) {
            if (p.lifetime <= 0f) {
                p.particleType = ParticleType.DUST_CLOUD
                p.position.set(
                    origin.x + (kotlin.random.Random.nextFloat() - 0.5f) * 0.3f,
                    origin.y + 0.04f,
                    origin.z + 0.15f
                )
                p.velocity.set(
                    (kotlin.random.Random.nextFloat() - 0.5f) * 1.8f,
                    kotlin.random.Random.nextFloat() * 1.8f + 0.4f,
                    kotlin.random.Random.nextFloat() * 2.2f + 0.8f
                )
                p.lifetime = 0.26f + kotlin.random.Random.nextFloat() * 0.16f
                p.maxLife = p.lifetime
                p.size = 0.07f + kotlin.random.Random.nextFloat() * 0.05f
                p.growthRate = 0.20f
                p.color[0] = 0.85f; p.color[1] = 0.85f; p.color[2] = 0.90f; p.color[3] = 0.22f
                emitted++
                if (emitted >= count) break
            }
        }
    }

    fun emitSpeedStreak(origin: Vector3, count: Int, baseColor: FloatArray) {
        var emitted = 0
        for (p in particles) {
            if (p.lifetime <= 0f) {
                p.particleType = ParticleType.SPEED_STREAK
                p.position.set(
                    origin.x + (kotlin.random.Random.nextFloat() - 0.5f) * 1.0f,
                    origin.y + kotlin.random.Random.nextFloat() * 1.5f,
                    origin.z - kotlin.random.Random.nextFloat() * 2.0f
                )
                p.velocity.set(0f, 0f, 8f)
                p.lifetime = 0.22f
                p.maxLife = 0.22f
                p.size = 0.10f
                p.growthRate = 0f
                p.color[0] = baseColor[0]; p.color[1] = baseColor[1]; p.color[2] = baseColor[2]; p.color[3] = 0.8f
                emitted++
                if (emitted >= count) break
            }
        }
    }

    fun emitShockwave(origin: Vector3, count: Int, baseColor: FloatArray) {
        var emitted = 0
        for (p in particles) {
            if (p.lifetime <= 0f) {
                p.particleType = ParticleType.SHOCKWAVE
                val angle = (emitted.toFloat() / count.toFloat()) * (2f * PI.toFloat())
                val speed = 9f
                p.position.set(origin.x, origin.y + 0.08f, origin.z)
                p.velocity.set(kotlin.math.cos(angle) * speed, 0.4f, kotlin.math.sin(angle) * speed)
                p.lifetime = 0.45f
                p.maxLife = 0.45f
                p.size = 0.22f
                p.growthRate = 2.4f
                p.color[0] = baseColor[0]; p.color[1] = baseColor[1]; p.color[2] = baseColor[2]; p.color[3] = 1f
                emitted++
                if (emitted >= count) break
            }
        }
    }

    fun update(dt: Float, worldScrollZ: Float = 0f) {
        for (p in particles) {
            if (p.lifetime > 0f) {
                p.lifetime -= dt
                p.position.z += worldScrollZ

                when (p.particleType) {
                    ParticleType.DUST_CLOUD -> {
                        // Soft billowing dust expands horizontally along the ground without rising to occlude incoming balls
                        p.size += p.growthRate * dt
                        p.velocity.x *= (1f - dt * 2.2f).coerceAtLeast(0f)
                        p.velocity.z *= (1f - dt * 2.2f).coerceAtLeast(0f)
                        p.velocity.y *= (1f - dt * 3.0f).coerceAtLeast(0f)
                        p.position.x += p.velocity.x * dt
                        p.position.y = (p.position.y + p.velocity.y * dt).coerceIn(0.04f, 0.38f)
                        p.position.z += p.velocity.z * dt
                        p.rotation += p.rotSpeed * dt
                        val lifeRatio = (p.lifetime / p.maxLife).coerceIn(0f, 1f)
                        p.color[3] = lifeRatio * 0.32f
                    }
                    ParticleType.ROCK_DEBRIS -> {
                        // Tumbling rock fragments with realistic gravity & asphalt bounce
                        p.rotation += p.rotSpeed * dt
                        p.velocity.y -= 22f * dt // Gravity
                        p.position.x += p.velocity.x * dt
                        p.position.y += p.velocity.y * dt
                        p.position.z += p.velocity.z * dt

                        // Ground contact bounce
                        if (p.position.y <= 0.05f && p.velocity.y < 0f) {
                            p.position.y = 0.05f
                            p.velocity.y = -p.velocity.y * p.bounciness
                            p.velocity.x *= 0.60f
                            p.velocity.z *= 0.60f
                        }
                        val alphaFactor = (p.lifetime / p.maxLife).coerceIn(0f, 1f)
                        p.color[3] = if (alphaFactor < 0.25f) (alphaFactor / 0.25f) * 0.80f else 0.80f
                    }
                    ParticleType.SHOCKWAVE -> {
                        p.size += p.growthRate * dt
                        p.position.x += p.velocity.x * dt
                        p.position.z += p.velocity.z * dt
                        p.color[3] = (p.lifetime / p.maxLife).coerceIn(0f, 1f) * 0.70f
                    }
                    ParticleType.SPARK, ParticleType.SPEED_STREAK -> {
                        p.position.x += p.velocity.x * dt
                        p.position.y += p.velocity.y * dt
                        p.position.z += p.velocity.z * dt
                        p.velocity.y -= 14f * dt
                        p.color[3] = (p.lifetime / p.maxLife).coerceIn(0f, 1f) * 0.75f
                    }
                }
            }
        }
    }
}

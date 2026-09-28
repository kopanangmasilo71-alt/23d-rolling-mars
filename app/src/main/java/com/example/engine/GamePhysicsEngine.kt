package com.example.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GamePhysicsEngine(
    val audio: GameAudio,
    val onGameOver: (score: Int, distanceMeters: Int, ballsDodged: Int, maxCombo: Int, sectorName: String) -> Unit
) {
    val roadWidth = 12.0f
    val roadHalfWidth = roadWidth / 2f

    val player = PlayerCharacter()
    val ballPool = Array(32) { RollingBall(it) }
    val speedPadPool = Array(6) { SpeedPad(it) }
    val collectiblePool = Array(12) { CollectibleItem(it) }

    // Road segments: 10 segments of 30 meters = 300m road view
    // Start from +60m so the road extends well behind the camera (cam at z=7m)
    val segmentLength = 30f
    val numSegments = 10
    val roadSegments = Array(numSegments) { i ->
        RoadSegment(i, 60f - i * segmentLength, segmentLength)
    }

    // Roadside scenery objects (pine trees, cyber light towers, rock monoliths, floating crystals, sci-fi pyramids)
    val scenery = ArrayList<SceneryItem>()

    val particles = ParticleSystem(750)
    val projectilePool = Array(24) { Projectile(it) }
    var isShootHeld: Boolean = false
    var shootCooldownTimer: Float = 0f

    // Camera
    val cameraPos = Vector3(0f, 4.2f, 7.0f)
    val cameraLookAt = Vector3(0f, 1.2f, -14f)
    var cameraShakeMagnitude: Float = 0f

    // Gameplay state
    var isRunning: Boolean = false
    var isGameOver: Boolean = false
    var isOverdriveMode: Boolean = false
    var distanceTraveled: Float = 0f
    var ballsDodged: Int = 0
    var nearMissCount: Int = 0
    var score: Int = 0

    // Combo system
    var comboMultiplier: Int = 1
    var comboTimer: Float = 0f
    var maxComboThisRun: Int = 1
    val maxComboTimer: Float = 3.6f

    // Sector Progression System
    val sectors = listOf(
        SectorInfo(
            1, "SECTOR 1", "OUTPOST DAWN", 0f, 1.0f,
            floatArrayOf(0.06f, 0.08f, 0.16f),
            floatArrayOf(0.95f, 0.40f, 0.20f),
            floatArrayOf(0.38f, 0.40f, 0.48f),
            floatArrayOf(0.90f, 0.88f, 0.82f)
        ),
        SectorInfo(
            2, "SECTOR 2", "NEON CANYON", 150f, 1.15f,
            floatArrayOf(0.14f, 0.04f, 0.22f),
            floatArrayOf(1.00f, 0.15f, 0.55f),
            floatArrayOf(0.42f, 0.32f, 0.52f),
            floatArrayOf(1.00f, 0.70f, 0.85f)
        ),
        SectorInfo(
            3, "SECTOR 3", "HYPER GRID", 350f, 1.30f,
            floatArrayOf(0.02f, 0.15f, 0.18f),
            floatArrayOf(0.00f, 0.95f, 0.80f),
            floatArrayOf(0.30f, 0.48f, 0.50f),
            floatArrayOf(0.70f, 1.00f, 0.95f)
        ),
        SectorInfo(
            4, "SECTOR 4", "MAGMA CORE", 650f, 1.45f,
            floatArrayOf(0.18f, 0.05f, 0.03f),
            floatArrayOf(1.00f, 0.35f, 0.05f),
            floatArrayOf(0.50f, 0.32f, 0.30f),
            floatArrayOf(1.00f, 0.65f, 0.40f)
        ),
        SectorInfo(
            5, "SECTOR 5", "QUANTUM VOID", 1000f, 1.60f,
            floatArrayOf(0.08f, 0.04f, 0.18f),
            floatArrayOf(0.75f, 0.85f, 1.00f),
            floatArrayOf(0.40f, 0.38f, 0.55f),
            floatArrayOf(0.95f, 0.90f, 1.00f)
        )
    )

    var currentSector: SectorInfo = sectors[0]
    var orbsCollected: Int = 0
    val maxOrbsForBoost: Int = 3
    var onSectorChanged: ((SectorInfo) -> Unit)? = null
    var onNearMissEvent: (() -> Unit)? = null
    var onCollectibleCollected: ((CollectibleType) -> Unit)? = null
    var onShieldDeflected: (() -> Unit)? = null
    var onDodgeFeedback: ((text: String, scoreGain: Int, combo: Int) -> Unit)? = null
    var onShootFired: (() -> Unit)? = null
    var onBoulderDestroyed: (() -> Unit)? = null
    var onOutOfAmmo: (() -> Unit)? = null

    // Spawning control
    private var spawnTimer: Float = 0f
    private var nextSpawnInterval: Float = 2.0f
    private var speedPadTimer: Float = 0f
    private var collectibleTimer: Float = 0f
    private var gameTime: Float = 0f
    private var crashTimer: Float = 0f
    private var footstepTimer: Float = 0f

    init {
        generateInitialScenery()
    }

    private fun generateInitialScenery() {
        scenery.clear()
        // Distribute diverse sci-fi scenery along the roadside safely outside the road
        for (i in 0 until 48) {
            val z = -20f - i * 12f // Start 20m ahead of player
            val typeRoll = Random.nextFloat()
            val sceneryType = when {
                typeRoll < 0.28f -> 0 // Pine Tree
                typeRoll < 0.52f -> 1 // Cyber Light Tower
                typeRoll < 0.72f -> 2 // Rock Monolith Boulder
                typeRoll < 0.88f -> 3 // Floating Sci-Fi Crystal
                else -> 4 // Futuristic Pyramid
            }

            // Left verge - pyramids placed far away to prevent road overlap
            val leftDist = if (sceneryType == 4) (24f + Random.nextFloat() * 16f) else (9.5f + Random.nextFloat() * 12f)
            val leftScale = if (sceneryType == 4) 1.2f else (0.8f + Random.nextFloat() * 0.4f)
            scenery.add(
                SceneryItem(
                    x = -leftDist,
                    z = z + Random.nextFloat() * 6f,
                    type = sceneryType,
                    scale = leftScale,
                    rotationY = Random.nextFloat() * 360f
                )
            )

            // Right verge
            val rightType = Random.nextInt(5)
            val rightDist = if (rightType == 4) (24f + Random.nextFloat() * 16f) else (9.5f + Random.nextFloat() * 12f)
            val rightScale = if (rightType == 4) 1.2f else (0.8f + Random.nextFloat() * 0.4f)
            scenery.add(
                SceneryItem(
                    x = rightDist,
                    z = z + Random.nextFloat() * 6f,
                    type = rightType,
                    scale = rightScale,
                    rotationY = Random.nextFloat() * 360f
                )
            )
        }
    }

    fun startNewGame(characterColor: String = "Classic Blue", overdrive: Boolean = false) {
        isOverdriveMode = overdrive
        player.reset()
        player.applyColorPreset(characterColor)
        for (b in ballPool) {
            b.isActive = false
        }
        for (sp in speedPadPool) {
            sp.isActive = false
        }
        for (col in collectiblePool) {
            col.isActive = false
        }
        for (pr in projectilePool) {
            pr.isActive = false
        }
        isShootHeld = false
        shootCooldownTimer = 0f
        for (i in 0 until numSegments) {
            roadSegments[i].zStart = 60f - i * segmentLength
        }
        generateInitialScenery()

        cameraPos.set(0f, 4.2f, 7.0f)
        cameraLookAt.set(0f, 1.2f, -14f)
        cameraShakeMagnitude = 0f

        distanceTraveled = 0f
        ballsDodged = 0
        nearMissCount = 0
        score = 0
        orbsCollected = 0
        comboMultiplier = 1
        comboTimer = 0f
        maxComboThisRun = 1
        currentSector = sectors[0]

        gameTime = 0f
        spawnTimer = 0f
        speedPadTimer = 0f
        collectibleTimer = 0f
        crashTimer = 0f
        footstepTimer = 0f
        nextSpawnInterval = if (isOverdriveMode) 1.4f else 2.0f
        isRunning = true
        isGameOver = false

        // Spawn initial opening wave immediately so the player sees obstacles right away!
        val initSpeed = if (isOverdriveMode) 8.5f else 6.5f
        spawnBall(BallType.STRAIGHT, x = 0f, z = -32f, speed = initSpeed, lateralSpeed = 0f)
        spawnBall(BallType.LEFT_TO_RIGHT, x = -(roadHalfWidth - 2.0f), z = -56f, speed = initSpeed * 1.05f, lateralSpeed = 2.2f)
        spawnBall(BallType.STRAIGHT, x = 2.4f, z = -80f, speed = initSpeed * 1.1f, lateralSpeed = 0f)

        // Spawn initial golden Energy Orb & Shield in lanes!
        spawnCollectibleAt(CollectibleType.ENERGY_ORB, x = 0f, z = -18f)
        spawnCollectibleAt(CollectibleType.AMMO_PACK, x = 2.2f, z = -26f)
        spawnCollectibleAt(CollectibleType.SHIELD, x = -2.0f, z = -44f)
    }

    fun jump() {
        if (!isRunning || isGameOver) return
        if (player.jump()) {
            audio.playJump()
            particles.emitShockwave(
                Vector3(player.position.x, 0.05f, player.position.z),
                16,
                player.neonGlowColor
            )
        }
    }

    fun shoot() {
        if (!isRunning || isGameOver) return
        if (player.ammo <= 0) {
            if (player.emptyClickTimer <= 0f) {
                player.emptyClickTimer = 0.25f
                audio.playDryFire()
                cameraShakeMagnitude = (cameraShakeMagnitude + 0.05f).coerceAtMost(0.25f)
                onOutOfAmmo?.invoke()
                onDodgeFeedback?.invoke("NO AMMO! COLLECT POWER-UPS", 0, comboMultiplier)
            }
            return
        }

        if (shootCooldownTimer <= 0f) {
            shootCooldownTimer = 0.18f
            if (!player.consumeAmmo()) return

            for (p in projectilePool) {
                if (!p.isActive) {
                    val gunX = player.position.x + 0.32f
                    val gunY = player.position.y + 0.88f
                    val gunZ = player.position.z - 0.85f
                    p.reset(gunX, gunY, gunZ)

                    audio.playLaserShot()
                    particles.emitBurst(Vector3(gunX, gunY, gunZ), 8, floatArrayOf(0.0f, 0.95f, 1.0f))
                    player.isShooting = true
                    player.muzzleFlashTimer = 0.12f
                    player.shootRecoil = 0.28f

                    // Tactile Screen Shake on Shot!
                    cameraShakeMagnitude = (cameraShakeMagnitude + 0.35f).coerceAtMost(0.70f)
                    onShootFired?.invoke()
                    break
                }
            }
        }
    }

    fun update(
        dt: Float,
        leftHeld: Boolean,
        rightHeld: Boolean,
        brakeHeld: Boolean
    ) {
        val clampedDt = dt.coerceIn(0.001f, 0.05f)

        // Game over tumbling physics
        if (isGameOver) {
            player.update(clampedDt, false, false, false, roadHalfWidth)
            particles.update(clampedDt)
            cameraShakeMagnitude = (cameraShakeMagnitude - clampedDt * 3.5f).coerceAtLeast(0f)
            crashTimer += clampedDt
            if (crashTimer >= 0.85f && isRunning) {
                isRunning = false
                onGameOver(score, distanceTraveled.toInt(), ballsDodged, maxComboThisRun, currentSector.name)
            }
            return
        }

        if (!isRunning) {
            particles.update(clampedDt)
            return
        }

        gameTime += clampedDt

        // Gun auto-fire while held and recoil cooldown
        if (shootCooldownTimer > 0f) {
            shootCooldownTimer -= clampedDt
        }
        if (isShootHeld && shootCooldownTimer <= 0f) {
            shoot()
        }
        player.isShooting = isShootHeld || shootCooldownTimer > 0.04f

        // 1. Update Sector progression
        checkSectorProgression()

        // 2. Update Combo Timer decay
        if (comboTimer > 0f) {
            comboTimer -= clampedDt
            if (comboTimer <= 0f) {
                comboMultiplier = 1
            }
        }

        // 3. Update Player
        player.update(clampedDt, leftHeld, rightHeld, brakeHeld, roadHalfWidth)

        // Landing impact feel: compression, ground shockwave, sound, and camera dip
        if (player.justLanded) {
            audio.playLandingImpact()
            particles.emitShockwave(
                Vector3(player.position.x, 0.05f, player.position.z),
                28,
                floatArrayOf(0.85f, 0.88f, 0.95f, 0.85f)
            )
            cameraShakeMagnitude = (cameraShakeMagnitude + 0.22f).coerceAtMost(0.45f)
        }

        // Footstep dust generation while running on ground
        if (player.isGrounded) {
            footstepTimer += clampedDt * (player.forwardSpeed / player.baseNormalSpeed) * 12f
            if (footstepTimer >= 3.14159f) {
                footstepTimer -= 3.14159f
                particles.emitDust(player.position, 3)
            }
        }

        // 4. Road scrolling & distance tracking
        val sectorSpeedMult = currentSector.speedMultiplier * (if (isOverdriveMode) 1.25f else 1.0f)
        val forwardDelta = player.forwardSpeed * sectorSpeedMult * clampedDt
        distanceTraveled += forwardDelta

        // Dynamic Score Accumulation (with 2X Multiplier power-up support)
        val difficultyBonus = if (isOverdriveMode) 2 else 1
        val multiplierBonus = if (player.isScoreBoosted) player.scoreMultiplierValue else 1
        val distanceScoreGain = (forwardDelta * 12f * comboMultiplier * difficultyBonus * multiplierBonus).toInt()
        score += distanceScoreGain

        // Recycle road segments
        for (seg in roadSegments) {
            seg.zStart += forwardDelta
            if (seg.zStart > 60f + segmentLength) {
                var minZ = 0f
                for (s in roadSegments) {
                    if (s.zStart < minZ) minZ = s.zStart
                }
                seg.zStart = minZ - segmentLength
            }
        }

        // Move and recycle scenery
        for (i in scenery.indices) {
            val item = scenery[i]
            val newZ = item.z + forwardDelta
            if (newZ > 30f) {
                val recycledZ = newZ - (48 * 12f)
                val isLeft = item.x < 0
                val newType = Random.nextInt(5)
                val dist = if (newType == 4) (24f + Random.nextFloat() * 16f) else (9.5f + Random.nextFloat() * 12f)
                val newX = if (isLeft) -dist else dist
                val scale = if (newType == 4) 1.2f else (0.8f + Random.nextFloat() * 0.4f)
                scenery[i] = item.copy(x = newX, z = recycledZ, type = newType, scale = scale, rotationY = Random.nextFloat() * 360f)
            } else {
                scenery[i] = item.copy(z = newZ)
            }
        }

        // 5. Update Speed Pads
        for (pad in speedPadPool) {
            if (pad.isActive) {
                pad.position.z += forwardDelta
                pad.update(clampedDt)

                // Check collision with player
                if (abs(player.position.x - pad.position.x) < 1.6f &&
                    abs(player.position.z - pad.position.z) < 1.8f &&
                    player.position.y < 0.6f
                ) {
                    pad.isActive = false
                    player.applySpeedBoost(3.0f)
                    audio.playSpeedPad()
                    score += 150 * comboMultiplier * difficultyBonus * multiplierBonus
                    particles.emitShockwave(
                        Vector3(pad.position.x, 0.05f, pad.position.z),
                        24,
                        floatArrayOf(0.0f, 0.95f, 1.0f)
                    )
                    cameraShakeMagnitude = 0.20f
                }

                if (pad.position.z > 20f) {
                    pad.isActive = false
                }
            }
        }

        // Spawn Speed Pads in Sector 2+
        if (currentSector.id >= 2) {
            speedPadTimer += clampedDt
            if (speedPadTimer >= (if (isOverdriveMode) 8f else 12f)) {
                speedPadTimer = 0f
                spawnSpeedPad()
            }
        }

        // 5b. Update Collectible Power-Ups
        for (col in collectiblePool) {
            if (col.isActive) {
                col.position.z += forwardDelta
                col.update(clampedDt)

                // Check pickup collision with player (radius ~0.65m, player torso ~0.6m)
                val latDist = abs(player.position.x - col.position.x)
                val longDist = abs(player.position.z - col.position.z)
                val vertDist = abs(player.position.y + 0.6f - col.position.y)

                if (latDist < 1.35f && longDist < 1.45f && vertDist < 1.5f) {
                    col.isActive = false
                    col.position.set(0f, -200f, 0f)
                    when (col.type) {
                        CollectibleType.AMMO_PACK -> {
                            player.addAmmo(10)
                            audio.playAmmoPickup()
                            score += 200 * comboMultiplier * difficultyBonus * multiplierBonus
                            particles.emitShockwave(
                                Vector3(player.position.x, 0.05f, player.position.z),
                                26,
                                col.type.color
                            )
                            onDodgeFeedback?.invoke("+10 PLASMA AMMO!", 200, comboMultiplier)
                        }
                        CollectibleType.SHIELD -> {
                            player.activateShield(14f)
                            player.addAmmo(4)
                            audio.playShieldPickup()
                            particles.emitShockwave(
                                Vector3(player.position.x, 0.05f, player.position.z),
                                28,
                                col.type.color
                            )
                            onDodgeFeedback?.invoke("SHIELD +4 AMMO!", 100, comboMultiplier)
                        }
                        CollectibleType.SPEED_BOOST -> {
                            player.applySpeedBoost(6.0f)
                            player.addAmmo(5)
                            audio.playSpeedPad()
                            particles.emitShockwave(
                                Vector3(player.position.x, 0.05f, player.position.z),
                                20,
                                col.type.color
                            )
                            onDodgeFeedback?.invoke("BOOST +5 AMMO!", 100, comboMultiplier)
                        }
                        CollectibleType.SCORE_MULTIPLIER -> {
                            player.activateScoreMultiplier(9.0f, 2)
                            player.addAmmo(5)
                            audio.playPowerUpPickup()
                            particles.emitShockwave(
                                Vector3(player.position.x, 0.05f, player.position.z),
                                24,
                                col.type.color
                            )
                            onDodgeFeedback?.invoke("2X MULTIPLIER +5 AMMO!", 150, comboMultiplier)
                        }
                        CollectibleType.ENERGY_CELL -> {
                            score += 150 * comboMultiplier * difficultyBonus * multiplierBonus
                            comboTimer = maxComboTimer
                            player.addAmmo(6)
                            audio.playAmmoPickup()
                            onDodgeFeedback?.invoke("+6 AMMO & REFILL!", 150, comboMultiplier)
                        }
                        CollectibleType.ENERGY_ORB -> {
                            orbsCollected++
                            player.addAmmo(2)
                            audio.playOrbCollect(orbsCollected)
                            score += 100 * comboMultiplier * difficultyBonus * multiplierBonus
                            if (orbsCollected >= maxOrbsForBoost) {
                                orbsCollected = 0
                                player.applySpeedBoost(3.0f)
                                player.addAmmo(6)
                                audio.playBoostSurge()
                                cameraShakeMagnitude = 0.35f
                                particles.emitShockwave(
                                    Vector3(player.position.x, 0.05f, player.position.z),
                                    32,
                                    floatArrayOf(0.0f, 0.95f, 1.0f, 1.0f)
                                )
                                onDodgeFeedback?.invoke("2x SPEED BOOST +6 AMMO!", 300, comboMultiplier)
                            }
                        }
                    }

                    particles.emitBurst(
                        Vector3(player.position.x, 0.05f, player.position.z),
                        12,
                        col.type.color
                    )
                    cameraShakeMagnitude = (cameraShakeMagnitude + 0.12f).coerceAtMost(0.35f)
                    onCollectibleCollected?.invoke(col.type)
                }

                if (col.position.z > 22f) {
                    col.isActive = false
                }
            }
        }

        // Periodic Collectible Spawning (every 5-7 seconds)
        collectibleTimer += clampedDt
        val targetCollectibleInterval = if (isOverdriveMode) 5.5f else 6.8f
        if (collectibleTimer >= targetCollectibleInterval) {
            collectibleTimer = 0f
            spawnRandomCollectible()
        }

        // 5b. Update Laser Projectiles & Boulder Blast Collisions
        for (proj in projectilePool) {
            if (proj.isActive) {
                proj.update(clampedDt)
                proj.trailTimer += clampedDt
                if (proj.trailTimer >= 0.035f) {
                    proj.trailTimer = 0f
                    particles.emitSpeedStreak(proj.position, 1, floatArrayOf(0.0f, 0.95f, 1.0f, 0.85f))
                }

                // Check collision with rolling boulders
                for (ball in ballPool) {
                    if (ball.isActive) {
                        val dx = proj.position.x - ball.position.x
                        val dy = proj.position.y - ball.position.y
                        val dz = proj.position.z - ball.position.z
                        val distSq = dx * dx + dy * dy + dz * dz
                        val hitRadius = ball.radius + proj.radius
                        if (distSq < hitRadius * hitRadius) {
                            // Target destroyed!
                            ball.isActive = false
                            proj.isActive = false
                            ballsDodged++
                            val destroyScore = 250 * comboMultiplier * difficultyBonus * multiplierBonus
                            score += destroyScore
                            comboTimer = maxComboTimer // Refresh combo on successful hit

                            audio.playBoulderExplode()
                            particles.emitBoulderLaserShatter(ball.position, ball.radius, ball.colorA)
                            cameraShakeMagnitude = (cameraShakeMagnitude + 0.70f).coerceAtMost(1.05f)
                            onBoulderDestroyed?.invoke()
                            onDodgeFeedback?.invoke("TARGET BLASTED! +250", 250, comboMultiplier)
                            break
                        }
                    }
                }

                if (proj.position.z < -65f) {
                    proj.isActive = false
                }
            }
        }

        // 6. Update Rolling Boulders
        for (ball in ballPool) {
            if (ball.isActive) {
                ball.update(clampedDt)
                ball.position.z += forwardDelta

                // 6a. Continuous Rolling Ground Dust & Debris Trail (billowing dust clouds)
                val isNearGround = ball.position.y <= (ball.radius + 0.15f)
                if (isNearGround) {
                    ball.trailDustTimer += clampedDt
                    val dustInterval = if (ball.ballType == BallType.GIANT) 0.045f else 0.055f
                    if (ball.trailDustTimer >= dustInterval) {
                        ball.trailDustTimer = 0f
                        particles.emitBoulderTrailDust(ball.position, ball.radius, ball.forwardVelocity, ball.colorA)
                    }
                }

                // 6b. Bouncing Boulder Ground Impact Shockwaves & Dust Puffs
                if (ball.ballType == BallType.BOUNCING) {
                    val isGroundedNow = ball.position.y <= (ball.radius + 0.08f)
                    if (isGroundedNow && ball.wasInAir) {
                        particles.emitBoulderImpact(ball.position, ball.radius, ball.colorA)
                        if (abs(ball.position.z - player.position.z) < 18f) {
                            cameraShakeMagnitude = (cameraShakeMagnitude + 0.14f).coerceAtMost(0.65f)
                        }
                    }
                    ball.wasInAir = !isGroundedNow
                }

                // 6c. Barrier / Curb Collision Detection (Concrete sparks, dust plumes & bounce)
                if (ball.curbCooldownTimer > 0f) {
                    ball.curbCooldownTimer -= clampedDt
                }
                val curbLeft = -roadHalfWidth + ball.radius
                val curbRight = roadHalfWidth - ball.radius

                if (ball.position.x <= curbLeft) {
                    ball.position.x = curbLeft
                    ball.horizontalVelocity = kotlin.math.abs(ball.horizontalVelocity) * 0.70f
                    if (ball.curbCooldownTimer <= 0f) {
                        ball.curbCooldownTimer = 0.20f
                        particles.emitCurbCollision(ball.position, isLeftSide = true, ball.colorA)
                        if (abs(ball.position.z - player.position.z) < 20f) {
                            cameraShakeMagnitude = (cameraShakeMagnitude + 0.16f).coerceAtMost(0.65f)
                        }
                    }
                } else if (ball.position.x >= curbRight) {
                    ball.position.x = curbRight
                    ball.horizontalVelocity = -kotlin.math.abs(ball.horizontalVelocity) * 0.70f
                    if (ball.curbCooldownTimer <= 0f) {
                        ball.curbCooldownTimer = 0.20f
                        particles.emitCurbCollision(ball.position, isLeftSide = false, ball.colorA)
                        if (abs(ball.position.z - player.position.z) < 20f) {
                            cameraShakeMagnitude = (cameraShakeMagnitude + 0.16f).coerceAtMost(0.65f)
                        }
                    }
                }

                // Check Near Miss (Player brushes right past the boulder)
                if (!ball.hasDodged && abs(ball.position.z - player.position.z) < 1.0f) {
                    val latDist = abs(ball.position.x - player.position.x)
                    val clearance = ball.radius + (player.torsoWidth / 2f)
                    if (latDist > clearance && latDist < (clearance + 0.95f)) {
                        // Near miss triggered!
                        ball.hasDodged = true
                        ballsDodged++
                        nearMissCount++
                        triggerNearMiss(ball)
                    }
                }

                // Check Standard Safe Dodge
                if (!ball.hasDodged && ball.position.z > (player.position.z + 1.3f)) {
                    ball.hasDodged = true
                    ballsDodged++
                    onSuccessfulDodge()
                }

                // Deactivate when past camera
                if (ball.position.z > 22f) {
                    ball.isActive = false
                }

                // Collision Check with Shield and Invulnerability Defense
                if (checkCollision(player, ball)) {
                    if (player.isShieldActive) {
                        // SHIELD ABSORBS IMPACT! Player deflects boulder and continues running!
                        player.breakShield(gracePeriod = 1.6f)
                        ball.isActive = false
                        audio.playShieldDeflect()
                        cameraShakeMagnitude = 0.65f
                        val multBonus = if (player.isScoreBoosted) player.scoreMultiplierValue else 1
                        val diffBonus = if (isOverdriveMode) 2 else 1
                        score += 350 * comboMultiplier * diffBonus * multBonus
                        ballsDodged++

                        // Shimmering explosion of pulverized rock and shield energy
                        particles.emitBoulderImpact(ball.position, ball.radius * 1.4f, floatArrayOf(0.0f, 0.95f, 1.0f, 1f))
                        particles.emitShockwave(Vector3(player.position.x, 0.1f, player.position.z), 32, floatArrayOf(0f, 0.95f, 1f, 1f))
                        particles.emitBurst(Vector3(player.position.x, player.position.y + 0.8f, player.position.z), 40, floatArrayOf(0f, 0.95f, 1f, 1f))

                        onShieldDeflected?.invoke()
                    } else if (player.invincibleGraceTimer > 0f) {
                        // Invulnerability grace period: boulder harmlessly shatters
                        ball.isActive = false
                        particles.emitShockwave(Vector3(player.position.x, 0.1f, player.position.z), 16, floatArrayOf(0f, 0.95f, 1f, 1f))
                    } else {
                        triggerGameOver(ball)
                        return
                    }
                }
            }
        }

        // 7. Procedural Ball Spawning
        spawnTimer += clampedDt
        if (spawnTimer >= nextSpawnInterval) {
            spawnTimer = 0f
            spawnObstacleWave()
        }

        // 8. Update Particles (synchronized with forward road scroll)
        particles.update(clampedDt, forwardDelta)

        // 9. Camera Shake Decay & Follow Camera
        if (cameraShakeMagnitude > 0f) {
            cameraShakeMagnitude = (cameraShakeMagnitude - clampedDt * 2.8f).coerceAtLeast(0f)
        }

        val shakeX = (Random.nextFloat() - 0.5f) * cameraShakeMagnitude
        val shakeY = (Random.nextFloat() - 0.5f) * cameraShakeMagnitude

        val targetCamX = player.position.x + shakeX
        val speedCamPullback = (player.forwardSpeed / player.baseNormalSpeed - 1f) * 1.5f
        val targetCamY = player.position.y * 0.40f + 3.1f + player.bodyBobOffset * 0.4f + shakeY
        val targetCamZ = player.position.z + 5.2f + speedCamPullback

        val xLerp = (28.0f * clampedDt).coerceAtMost(1f)
        val yzLerp = (12.0f * clampedDt).coerceAtMost(1f)
        cameraPos.x += (targetCamX - cameraPos.x) * xLerp
        cameraPos.y += (targetCamY - cameraPos.y) * yzLerp
        cameraPos.z += (targetCamZ - cameraPos.z) * yzLerp

        cameraLookAt.x = cameraPos.x
        cameraLookAt.y = 1.35f + player.position.y * 0.35f
        cameraLookAt.z = player.position.z - 22f
    }

    private fun checkSectorProgression() {
        var highestSector = sectors[0]
        for (sec in sectors) {
            if (distanceTraveled >= sec.minDistance) {
                highestSector = sec
            }
        }

        if (highestSector.id != currentSector.id) {
            currentSector = highestSector
            audio.playSectorAlert()
            cameraShakeMagnitude = 0.45f
            particles.emitShockwave(
                Vector3(player.position.x, 1.2f, player.position.z),
                32,
                currentSector.fogHorizonColor
            )
            onSectorChanged?.invoke(currentSector)
        }
    }

    private fun onSuccessfulDodge() {
        // Boost combo
        comboTimer = maxComboTimer
        if (comboMultiplier < 5) {
            comboMultiplier++
        }
        if (comboMultiplier > maxComboThisRun) {
            maxComboThisRun = comboMultiplier
        }
        audio.playCombo(comboMultiplier)

        val difficultyBonus = if (isOverdriveMode) 2 else 1
        val multBonus = if (player.isScoreBoosted) player.scoreMultiplierValue else 1
        val gain = 50 * comboMultiplier * difficultyBonus * multBonus
        score += gain
        if (comboMultiplier >= 4) {
            onDodgeFeedback?.invoke("PERFECT DODGE! +$gain", gain, comboMultiplier)
        } else {
            onDodgeFeedback?.invoke("DODGED! +$gain", gain, comboMultiplier)
        }
    }

    private fun triggerNearMiss(ball: RollingBall) {
        audio.playNearMiss()
        cameraShakeMagnitude = 0.30f
        player.nearMissTilt = if (ball.position.x > player.position.x) -14f else 14f

        comboTimer = maxComboTimer
        if (comboMultiplier < 5) comboMultiplier++
        if (comboMultiplier > maxComboThisRun) maxComboThisRun = comboMultiplier

        val difficultyBonus = if (isOverdriveMode) 2 else 1
        val multBonus = if (player.isScoreBoosted) player.scoreMultiplierValue else 1
        val gain = 100 * comboMultiplier * difficultyBonus * multBonus
        score += gain

        particles.emitBurst(
            Vector3(
                (player.position.x + ball.position.x) / 2f,
                player.position.y + 0.8f,
                player.position.z
            ),
            18,
            floatArrayOf(0.0f, 0.95f, 1.0f)
        )
        onDodgeFeedback?.invoke("NEAR MISS! +$gain", gain, comboMultiplier)
        onNearMissEvent?.invoke()
    }

    fun spawnCollectibleAt(type: CollectibleType, x: Float, z: Float) {
        for (col in collectiblePool) {
            if (!col.isActive) {
                col.reset(type, x, z)
                break
            }
        }
    }

    fun spawnRandomCollectible() {
        val roll = Random.nextFloat()
        val type = when {
            roll < 0.28f -> CollectibleType.AMMO_PACK // Dedicated Plasma Ammo Battery!
            roll < 0.50f -> CollectibleType.ENERGY_ORB // Golden Energy Orb!
            roll < 0.66f -> CollectibleType.SHIELD
            roll < 0.78f -> CollectibleType.SPEED_BOOST
            roll < 0.89f -> CollectibleType.SCORE_MULTIPLIER
            else -> CollectibleType.ENERGY_CELL
        }

        val laneChoice = Random.nextInt(3)
        val spawnX = when (laneChoice) {
            0 -> -3.2f
            1 -> 0.0f
            else -> 3.2f
        } + (Random.nextFloat() - 0.5f) * 0.8f

        val spawnZ = -80f - Random.nextFloat() * 20f
        spawnCollectibleAt(type, spawnX, spawnZ)
    }

    fun clearAllTrackEntities() {
        for (b in ballPool) {
            b.isActive = false
        }
        for (col in collectiblePool) {
            col.isActive = false
        }
        for (sp in speedPadPool) {
            sp.isActive = false
        }
    }

    private fun spawnSpeedPad() {
        for (pad in speedPadPool) {
            if (!pad.isActive) {
                val padX = (Random.nextFloat() - 0.5f) * (roadHalfWidth * 1.2f)
                val padZ = -75f - Random.nextFloat() * 15f
                pad.reset(padX, padZ)
                break
            }
        }
    }

    private fun spawnObstacleWave() {
        val dist = distanceTraveled
        val overdriveFactor = if (isOverdriveMode) 1.25f else 1.0f

        val baseSpeed = when {
            dist < 60f -> 6.5f + Random.nextFloat() * 1.5f
            dist < 180f -> 8.5f + Random.nextFloat() * 2.0f
            dist < 400f -> 10.5f + Random.nextFloat() * 2.5f
            dist < 800f -> 12.5f + Random.nextFloat() * 3.0f
            else -> 14.5f + Random.nextFloat() * 3.5f
        } * overdriveFactor

        nextSpawnInterval = when {
            dist < 60f -> 2.4f + Random.nextFloat() * 0.5f
            dist < 180f -> 1.9f + Random.nextFloat() * 0.4f
            dist < 400f -> 1.45f + Random.nextFloat() * 0.35f
            dist < 800f -> 1.15f + Random.nextFloat() * 0.3f
            else -> 0.90f + Random.nextFloat() * 0.22f
        } / (if (isOverdriveMode) 1.25f else 1.0f)

        val spawnZ = -90f - Random.nextFloat() * 15f
        val roll = Random.nextFloat()

        val type = when {
            dist < 50f -> BallType.STRAIGHT
            dist < 150f -> if (roll < 0.60f) BallType.STRAIGHT else if (roll < 0.80f) BallType.LEFT_TO_RIGHT else BallType.RIGHT_TO_LEFT
            dist < 350f -> when {
                roll < 0.35f -> BallType.STRAIGHT
                roll < 0.55f -> BallType.LEFT_TO_RIGHT
                roll < 0.75f -> BallType.RIGHT_TO_LEFT
                roll < 0.90f -> BallType.FAST
                else -> BallType.BOUNCING
            }
            else -> when {
                roll < 0.20f -> BallType.STRAIGHT
                roll < 0.40f -> BallType.LEFT_TO_RIGHT
                roll < 0.60f -> BallType.RIGHT_TO_LEFT
                roll < 0.76f -> BallType.FAST
                roll < 0.90f -> BallType.GIANT
                else -> BallType.BOUNCING
            }
        }

        val lateralSpeed = when (type) {
            BallType.LEFT_TO_RIGHT -> Random.nextFloat() * 2.8f + 1.4f
            BallType.RIGHT_TO_LEFT -> -(Random.nextFloat() * 2.8f + 1.4f)
            else -> 0f
        }

        val spawnX = when (type) {
            BallType.LEFT_TO_RIGHT -> -(roadHalfWidth - 1.8f)
            BallType.RIGHT_TO_LEFT -> (roadHalfWidth - 1.8f)
            BallType.GIANT -> (Random.nextFloat() - 0.5f) * (roadHalfWidth * 0.7f)
            else -> (Random.nextFloat() - 0.5f) * (roadHalfWidth * 1.5f)
        }

        spawnBall(type, spawnX, spawnZ, baseSpeed, lateralSpeed)

        // Dynamic twin hazard waves in higher difficulty
        val twinChance = if (isOverdriveMode) 0.50f else 0.30f
        if (dist > 220f && Random.nextFloat() < twinChance) {
            val otherX = if (spawnX < 0) spawnX + 5.2f else spawnX - 5.2f
            spawnBall(BallType.STRAIGHT, otherX, spawnZ - 12f, baseSpeed * 0.95f, 0f)
        }
    }

    private fun spawnBall(
        type: BallType,
        x: Float,
        z: Float,
        speed: Float,
        lateralSpeed: Float
    ) {
        for (b in ballPool) {
            if (!b.isActive) {
                b.reset(type, x, z, speed, lateralSpeed)
                break
            }
        }
    }

    private fun checkCollision(p: PlayerCharacter, b: RollingBall): Boolean {
        val hw = p.torsoWidth / 2f + 0.12f
        val minX = p.position.x - hw
        val maxX = p.position.x + hw
        val minY = p.position.y
        val maxY = p.position.y + p.totalHeight
        val minZ = p.position.z - 0.25f
        val maxZ = p.position.z + 0.25f

        val bx = b.position.x
        val by = b.position.y
        val bz = b.position.z
        val effectiveRadius = b.radius * 0.85f

        val cx = max(minX, min(bx, maxX))
        val cy = max(minY, min(by, maxY))
        val cz = max(minZ, min(bz, maxZ))

        val dx = bx - cx
        val dy = by - cy
        val dz = bz - cz

        return (dx * dx + dy * dy + dz * dz) < (effectiveRadius * effectiveRadius)
    }

    private fun triggerGameOver(hitBall: RollingBall) {
        isGameOver = true
        crashTimer = 0f
        player.triggerTumble()
        audio.playCrash()
        cameraShakeMagnitude = 1.1f

        // Violent explosion sparks & rock fragments
        particles.emitBurst(
            Vector3(player.position.x, player.position.y + 0.8f, player.position.z),
            60,
            hitBall.colorA
        )
        particles.emitShockwave(
            Vector3(player.position.x, 0.05f, player.position.z),
            32,
            floatArrayOf(1f, 0.2f, 0.1f)
        )
    }
}

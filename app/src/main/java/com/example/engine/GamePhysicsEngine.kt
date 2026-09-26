package com.example.engine

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GamePhysicsEngine(
    val audio: GameAudio,
    val onGameOver: (score: Int, distanceMeters: Int, ballsDodged: Int) -> Unit
) {
    val roadWidth = 12.0f
    val roadHalfWidth = roadWidth / 2f

    val player = PlayerCharacter()
    val ballPool = Array(32) { RollingBall(it) }

    // Road segments: 8 segments of 30 meters = 240m road view
    val segmentLength = 30f
    val numSegments = 8
    val roadSegments = Array(numSegments) { i ->
        RoadSegment(i, -i * segmentLength, segmentLength)
    }

    // Roadside scenery objects (pine trees, round trees, rocks)
    val scenery = ArrayList<SceneryItem>()

    val particles = ParticleSystem(100)

    // Camera
    val cameraPos = Vector3(0f, 4.2f, 7.0f)
    val cameraLookAt = Vector3(0f, 1.2f, -14f)

    // Gameplay state
    var isRunning: Boolean = false
    var isGameOver: Boolean = false
    var distanceTraveled: Float = 0f
    var ballsDodged: Int = 0
    var score: Int = 0

    // Spawning control
    private var spawnTimer: Float = 0f
    private var nextSpawnInterval: Float = 2.2f
    private var gameTime: Float = 0f

    init {
        generateInitialScenery()
    }

    private fun generateInitialScenery() {
        scenery.clear()
        // Distribute trees and rocks along the roadside
        for (i in 0 until 40) {
            val z = -i * 12f
            // Left verge (x between -8f and -18f)
            val leftX = -(Random.nextFloat() * 10f + 7.5f)
            scenery.add(
                SceneryItem(
                    x = leftX,
                    z = z + Random.nextFloat() * 4f,
                    type = Random.nextInt(3),
                    scale = 0.8f + Random.nextFloat() * 0.6f,
                    rotationY = Random.nextFloat() * 360f
                )
            )
            // Right verge (x between 8f and 18f)
            val rightX = Random.nextFloat() * 10f + 7.5f
            scenery.add(
                SceneryItem(
                    x = rightX,
                    z = z + Random.nextFloat() * 4f,
                    type = Random.nextInt(3),
                    scale = 0.8f + Random.nextFloat() * 0.6f,
                    rotationY = Random.nextFloat() * 360f
                )
            )
        }
    }

    fun startNewGame(characterColor: String = "Classic Blue") {
        player.reset()
        player.applyColorPreset(characterColor)
        for (b in ballPool) {
            b.isActive = false
        }
        for (i in 0 until numSegments) {
            roadSegments[i].zStart = -i * segmentLength
        }
        generateInitialScenery()

        cameraPos.set(0f, 4.2f, 7.0f)
        cameraLookAt.set(0f, 1.2f, -14f)

        distanceTraveled = 0f
        ballsDodged = 0
        score = 0
        gameTime = 0f
        spawnTimer = 0f
        nextSpawnInterval = 2.2f
        isRunning = true
        isGameOver = false
    }

    fun jump() {
        if (!isRunning || isGameOver) return
        if (player.jump()) {
            audio.playJump()
        }
    }

    fun update(
        dt: Float,
        leftHeld: Boolean,
        rightHeld: Boolean,
        brakeHeld: Boolean
    ) {
        if (!isRunning || isGameOver) {
            // Update particles even during game over
            particles.update(dt)
            return
        }

        val clampedDt = dt.coerceIn(0.001f, 0.05f)
        gameTime += clampedDt

        // Sound cue for braking
        if (brakeHeld && Random.nextFloat() < 0.12f) {
            audio.playBrake()
        }

        // 1. Update Player
        player.update(clampedDt, leftHeld, rightHeld, brakeHeld, roadHalfWidth)

        // 2. Road scrolling & distance tracking
        val forwardDelta = player.forwardSpeed * clampedDt
        distanceTraveled += forwardDelta
        score = (distanceTraveled * 10f).toInt() + (ballsDodged * 60)

        // Recycle road segments
        for (seg in roadSegments) {
            seg.zStart += forwardDelta
            if (seg.zStart > segmentLength) {
                // Find minimum zStart to place behind
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
            if (newZ > 15f) {
                val recycledZ = newZ - (40 * 12f)
                val newX = if (item.x < 0) -(Random.nextFloat() * 10f + 7.5f) else (Random.nextFloat() * 10f + 7.5f)
                scenery[i] = item.copy(x = newX, z = recycledZ, rotationY = Random.nextFloat() * 360f)
            } else {
                scenery[i] = item.copy(z = newZ)
            }
        }

        // 3. Update Balls
        for (ball in ballPool) {
            if (ball.isActive) {
                // The ball rolls forward relative to road plus player forward speed relative movement
                ball.update(clampedDt)
                // Add relative forward speed of road
                ball.position.z += forwardDelta

                // Check if ball passed player safely
                if (!ball.hasDodged && ball.position.z > (player.position.z + 1.2f)) {
                    ball.hasDodged = true
                    ballsDodged++
                    audio.playDodgeWhoosh()
                }

                // Deactivate when well behind camera
                if (ball.position.z > 20f) {
                    ball.isActive = false
                }

                // Collision Check
                if (checkCollision(player, ball)) {
                    triggerGameOver()
                    return
                }
            }
        }

        // 4. Procedural Ball Spawning
        spawnTimer += clampedDt
        if (spawnTimer >= nextSpawnInterval) {
            spawnTimer = 0f
            spawnObstacleWave()
        }

        // 5. Update Particles
        particles.update(clampedDt)

        // 6. Camera Follow: Directly behind player so player is ALWAYS centered & visible
        val targetCamX = player.position.x
        val targetCamY = player.position.y * 0.40f + 3.1f + player.bodyBobOffset * 0.4f
        val targetCamZ = player.position.z + 5.2f

        // High responsiveness in X so lateral dodges never leave the player behind or off-screen
        val xLerp = (28.0f * clampedDt).coerceAtMost(1f)
        val yzLerp = (12.0f * clampedDt).coerceAtMost(1f)
        cameraPos.x += (targetCamX - cameraPos.x) * xLerp
        cameraPos.y += (targetCamY - cameraPos.y) * yzLerp
        cameraPos.z += (targetCamZ - cameraPos.z) * yzLerp

        cameraLookAt.x = cameraPos.x
        cameraLookAt.y = 1.35f + player.position.y * 0.35f
        cameraLookAt.z = player.position.z - 22f
    }

    private fun spawnObstacleWave() {
        // Difficulty progression based on distance
        val dist = distanceTraveled
        val baseSpeed = when {
            dist < 60f -> 6.5f + Random.nextFloat() * 1.5f
            dist < 180f -> 8.5f + Random.nextFloat() * 2.0f
            dist < 400f -> 10.5f + Random.nextFloat() * 2.5f
            dist < 800f -> 12.5f + Random.nextFloat() * 3.0f
            else -> 14.5f + Random.nextFloat() * 3.5f
        }

        nextSpawnInterval = when {
            dist < 60f -> 2.5f + Random.nextFloat() * 0.6f
            dist < 180f -> 2.0f + Random.nextFloat() * 0.5f
            dist < 400f -> 1.5f + Random.nextFloat() * 0.4f
            dist < 800f -> 1.2f + Random.nextFloat() * 0.3f
            else -> 0.95f + Random.nextFloat() * 0.25f
        }

        val spawnZ = -90f - Random.nextFloat() * 15f
        val roll = Random.nextFloat()

        val type = when {
            dist < 50f -> BallType.STRAIGHT
            dist < 140f -> if (roll < 0.65f) BallType.STRAIGHT else if (roll < 0.85f) BallType.LEFT_TO_RIGHT else BallType.RIGHT_TO_LEFT
            dist < 320f -> when {
                roll < 0.35f -> BallType.STRAIGHT
                roll < 0.55f -> BallType.LEFT_TO_RIGHT
                roll < 0.75f -> BallType.RIGHT_TO_LEFT
                roll < 0.90f -> BallType.FAST
                else -> BallType.BOUNCING
            }
            else -> when {
                roll < 0.25f -> BallType.STRAIGHT
                roll < 0.45f -> BallType.LEFT_TO_RIGHT
                roll < 0.65f -> BallType.RIGHT_TO_LEFT
                roll < 0.80f -> BallType.FAST
                roll < 0.92f -> BallType.GIANT
                else -> BallType.BOUNCING
            }
        }

        // Lateral speed for crossing balls
        val lateralSpeed = when (type) {
            BallType.LEFT_TO_RIGHT -> Random.nextFloat() * 2.4f + 1.2f
            BallType.RIGHT_TO_LEFT -> -(Random.nextFloat() * 2.4f + 1.2f)
            else -> 0f
        }

        // Spawn position
        val spawnX = when (type) {
            BallType.LEFT_TO_RIGHT -> -(roadHalfWidth - 1.8f)
            BallType.RIGHT_TO_LEFT -> (roadHalfWidth - 1.8f)
            BallType.GIANT -> (Random.nextFloat() - 0.5f) * (roadHalfWidth * 0.8f)
            else -> (Random.nextFloat() - 0.5f) * (roadHalfWidth * 1.5f)
        }

        spawnBall(type, spawnX, spawnZ, baseSpeed, lateralSpeed)

        // Occasionally spawn twin balls in higher difficulty
        if (dist > 250f && Random.nextFloat() < 0.35f) {
            val otherX = if (spawnX < 0) spawnX + 4.5f else spawnX - 4.5f
            spawnBall(BallType.STRAIGHT, otherX, spawnZ - 10f, baseSpeed * 0.95f, 0f)
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
        // Player bounding box
        val hw = p.torsoWidth / 2f + 0.12f
        val minX = p.position.x - hw
        val maxX = p.position.x + hw
        val minY = p.position.y
        val maxY = p.position.y + p.totalHeight
        val minZ = p.position.z - 0.25f
        val maxZ = p.position.z + 0.25f

        // Ball center and effective collider radius (using 0.86 to ensure near misses feel fair)
        val bx = b.position.x
        val by = b.position.y
        val bz = b.position.z
        val effectiveRadius = b.radius * 0.86f

        // Find closest point on player box to ball center
        val cx = max(minX, min(bx, maxX))
        val cy = max(minY, min(by, maxY))
        val cz = max(minZ, min(bz, maxZ))

        val dx = bx - cx
        val dy = by - cy
        val dz = bz - cz

        return (dx * dx + dy * dy + dz * dz) < (effectiveRadius * effectiveRadius)
    }

    private fun triggerGameOver() {
        isGameOver = true
        isRunning = false
        audio.playCrash()
        // Emit spark burst at collision point
        particles.emitBurst(
            Vector3(player.position.x, player.position.y + 0.8f, player.position.z),
            40,
            floatArrayOf(1f, 0.4f, 0.1f)
        )
        onGameOver(score, distanceTraveled.toInt(), ballsDodged)
    }
}

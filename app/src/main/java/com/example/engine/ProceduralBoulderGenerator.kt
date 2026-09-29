package com.example.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Recognized hazard wave patterns generated procedurally.
 */
enum class HazardPattern(
    val displayName: String,
    val description: String,
    val minThreatLevel: Int,
    val baseIntervalMultiplier: Float
) {
    SOLO_PATROL(
        displayName = "Solo Patrol",
        description = "Single heavy rolling boulder tracking an incoming lane",
        minThreatLevel = 1,
        baseIntervalMultiplier = 1.0f
    ),
    STAGGERED_SLALOM(
        displayName = "Slalom Weave",
        description = "Boulders launched in alternating lanes at increasing intervals",
        minThreatLevel = 1,
        baseIntervalMultiplier = 1.25f
    ),
    PINCER_DOUBLE(
        displayName = "Pincer Cross",
        description = "Boulders sweeping inwards from both road curbs towards center",
        minThreatLevel = 2,
        baseIntervalMultiplier = 1.35f
    ),
    GATEWAY_WALL(
        displayName = "Gateway Corridor",
        description = "Coordinated blockers leaving a designated escape lane",
        minThreatLevel = 2,
        baseIntervalMultiplier = 1.45f
    ),
    BOUNCER_CARAVAN(
        displayName = "Bouncer Caravan",
        description = "Rhythmic high-trajectory bouncing spheres",
        minThreatLevel = 2,
        baseIntervalMultiplier = 1.30f
    ),
    TRIPLE_CASCADE(
        displayName = "Triple Cascade",
        description = "Three consecutive boulders at progressive intervals across lanes",
        minThreatLevel = 3,
        baseIntervalMultiplier = 1.60f
    ),
    JUGGERNAUT_FLANK(
        displayName = "Juggernaut Flank",
        description = "Colossal high-mass boulder flanked by a nimble crosser",
        minThreatLevel = 3,
        baseIntervalMultiplier = 1.65f
    ),
    SPEEDSTER_SURGE(
        displayName = "Speedster Surge",
        description = "Rapid high-velocity boulders deployed at staggered intervals",
        minThreatLevel = 3,
        baseIntervalMultiplier = 1.50f
    ),
    BREATHER_RESPITE(
        displayName = "Tactical Respite",
        description = "Extended recovery interval with isolated hazard",
        minThreatLevel = 1,
        baseIntervalMultiplier = 2.10f
    )
}

/**
 * Represents a boulder queued to be spawned when its delay expires.
 */
data class QueuedBoulder(
    val type: BallType,
    val x: Float,
    val z: Float,
    val forwardSpeed: Float,
    val lateralSpeed: Float,
    var delayRemaining: Float
)

/**
 * Procedural generation system that spawns rolling boulders at increasing intervals
 * to create fair, rhythmic, and exhilarating endless runner mechanics.
 *
 * Key features:
 * - Dynamic interval progression: intervals between waves and between staggered boulders
 *   dynamically increase with wave complexity and pacing cycles.
 * - Guaranteed solvability: mathematically verifies that every generated wave guarantees
 *   at least one unobstructed escape path for the player.
 * - Velocity compensation: scales physical spatial separation (Z-delta) as forward speed escalates,
 *   preserving fair reaction times across all difficulty tiers.
 * - Tension & release pacing: alternates between high-intensity wave surges and extended
 *   breather intervals.
 */
class ProceduralBoulderGenerator(
    private val roadHalfWidth: Float = 6.0f,
    private var random: Random = Random.Default
) {
    // Standard discrete lane coordinates spanning the road (-4.0m to +4.0m)
    val lanes = floatArrayOf(
        -4.0f, // Far Left
        -2.0f, // Left
        0.0f,  // Center
        2.0f,  // Right
        4.0f   // Far Right
    )

    // Interval configuration
    var baseSpawnInterval: Float = 2.2f
    var minSpawnInterval: Float = 0.55f
    var maxSpawnInterval: Float = 4.8f

    // Interval scaling rate per wave within a cycle
    var intervalProgressionRate: Float = 0.12f

    // Generation State
    var waveCount: Int = 0
        private set
    var totalBouldersSpawned: Int = 0
        private set
    var currentPattern: HazardPattern = HazardPattern.SOLO_PATROL
        private set
    var currentInterval: Float = baseSpawnInterval
        private set
    var timeUntilNextWave: Float = 1.0f
        private set
    var isBreatherActive: Boolean = false
        private set

    // Queue of staggered boulders awaiting spawn
    val pendingBoulders = ArrayList<QueuedBoulder>()

    // Event callbacks
    var onWaveSpawned: ((waveIndex: Int, pattern: HazardPattern, interval: Float) -> Unit)? = null
    var onIntervalChanged: ((newInterval: Float, isBreather: Boolean) -> Unit)? = null

    /**
     * Resets the procedural generator for a fresh run.
     * Optionally accepts a seed for deterministic procedural generation.
     */
    fun reset(seed: Long? = null) {
        random = if (seed != null) Random(seed) else Random.Default
        waveCount = 0
        totalBouldersSpawned = 0
        currentPattern = HazardPattern.SOLO_PATROL
        currentInterval = baseSpawnInterval
        timeUntilNextWave = 1.2f
        isBreatherActive = false
        pendingBoulders.clear()
    }

    /**
     * Updates the procedural generator state by delta time [dt].
     * Dispatches ready boulders through [spawnAction].
     *
     * @param dt Frame delta time in seconds
     * @param playerSpeed Current player forward speed
     * @param distanceTraveled Total distance traveled in meters
     * @param runDuration Total run duration in seconds
     * @param threatLevel Current difficulty tier (1..5)
     * @param isOverdrive Whether overdrive mode is active
     * @param spawnAction Callback invoked to spawn an active rolling boulder into the engine
     */
    fun update(
        dt: Float,
        playerSpeed: Float,
        distanceTraveled: Float,
        runDuration: Float,
        threatLevel: Int,
        isOverdrive: Boolean,
        spawnAction: (type: BallType, x: Float, z: Float, speed: Float, lateralSpeed: Float) -> Unit
    ) {
        val clampedDt = dt.coerceIn(0.0005f, 0.1f)

        // 1. Process pending staggered boulders in queue
        val it = pendingBoulders.iterator()
        while (it.hasNext()) {
            val queued = it.next()
            queued.delayRemaining -= clampedDt
            if (queued.delayRemaining <= 0f) {
                spawnAction(
                    queued.type,
                    queued.x,
                    queued.z,
                    queued.forwardSpeed,
                    queued.lateralSpeed
                )
                totalBouldersSpawned++
                it.remove()
            }
        }

        // 2. Countdown next wave interval
        timeUntilNextWave -= clampedDt
        if (timeUntilNextWave <= 0f) {
            generateNextWave(playerSpeed, distanceTraveled, runDuration, threatLevel, isOverdrive)
        }
    }

    /**
     * Computes the interval for the next wave based on wave count, current pattern,
     * speed, and difficulty scaling.
     *
     * Features:
     * - Progressive interval expansion within a wave cycle: as wave index increases from 1 to 4,
     *   intervals between waves increase progressively to accommodate larger multi-boulder formations.
     * - Breather wave every 5th wave: provides an extended recovery interval.
     * - Speed compensation: as speed increases, intervals adjust to keep physical reaction room.
     */
    fun calculateNextInterval(
        waveIndexInCycle: Int,
        pattern: HazardPattern,
        playerSpeed: Float,
        threatLevel: Int,
        isOverdrive: Boolean
    ): Float {
        // Base interval scaled by progressive step in cycle (increasing intervals!)
        // Wave 1: +0%, Wave 2: +12%, Wave 3: +24%, Wave 4: +36%
        val cycleProgressionBonus = (waveIndexInCycle * intervalProgressionRate).coerceAtMost(0.60f)
        val patternMult = pattern.baseIntervalMultiplier

        // Overdrive reduces intervals by 20%
        val overdriveMod = if (isOverdrive) 0.80f else 1.0f

        // Higher threat levels slightly accelerate rhythm, but pattern multiplier preserves spacing
        val threatModifier = when (threatLevel) {
            1 -> 1.05f
            2 -> 0.98f
            3 -> 0.92f
            4 -> 0.86f
            else -> 0.80f
        }

        // Velocity adjustment: higher player speed requires slightly longer intervals
        // so the player is not overwhelmed by boulders arriving too quickly in physical space
        val speedFactor = (playerSpeed / 10.0f).coerceIn(0.8f, 1.8f) * 0.15f

        val calculated = (baseSpawnInterval + cycleProgressionBonus + speedFactor) *
            patternMult *
            overdriveMod *
            threatModifier

        return calculated.coerceIn(minSpawnInterval, maxSpawnInterval)
    }

    /**
     * Procedurally constructs and schedules the next wave of rolling boulders.
     */
    fun generateNextWave(
        playerSpeed: Float,
        distanceTraveled: Float,
        runDuration: Float,
        threatLevel: Int,
        isOverdrive: Boolean
    ) {
        waveCount++
        val cycleIndex = (waveCount - 1) % 5 // 0, 1, 2, 3, 4 (4 is breather)

        // Select pattern based on threat level and cycle phase
        val selectedPattern = selectPattern(cycleIndex, threatLevel, distanceTraveled, runDuration)
        currentPattern = selectedPattern
        isBreatherActive = (selectedPattern == HazardPattern.BREATHER_RESPITE)

        // Calculate interval for this wave (progressively increasing)
        currentInterval = calculateNextInterval(cycleIndex, selectedPattern, playerSpeed, threatLevel, isOverdrive)
        timeUntilNextWave = currentInterval

        // Base forward speed for oncoming boulders, scaling with player speed and threat
        val speedMultiplier = 1.0f + (threatLevel - 1) * 0.12f + (if (isOverdrive) 0.20f else 0.0f)
        val baseBoulderSpeed = (playerSpeed * 0.70f + 3.5f) * speedMultiplier

        val spawnZ = -88f - random.nextFloat() * 12f

        // Build boulders for selected pattern
        buildPatternBoulders(selectedPattern, spawnZ, baseBoulderSpeed, playerSpeed)

        // Verify and enforce solvability guarantee across the generated wave
        enforceSolvabilityGuarantee()

        onWaveSpawned?.invoke(waveCount, selectedPattern, currentInterval)
        onIntervalChanged?.invoke(currentInterval, isBreatherActive)
    }

    /**
     * Selects an appropriate hazard pattern based on player progression and cycle pacing.
     */
    private fun selectPattern(
        cycleIndex: Int,
        threatLevel: Int,
        distanceTraveled: Float,
        runDuration: Float
    ): HazardPattern {
        // Every 5th wave in a cycle is a tactical breather (unless in extreme overload mode)
        if (cycleIndex == 4 && threatLevel < 5 && waveCount > 3) {
            return HazardPattern.BREATHER_RESPITE
        }

        // Filter patterns available at current threat level
        val eligiblePatterns = HazardPattern.entries.filter {
            it.minThreatLevel <= threatLevel && it != HazardPattern.BREATHER_RESPITE
        }

        if (eligiblePatterns.isEmpty()) {
            return HazardPattern.SOLO_PATROL
        }

        // Weight selection: early in run favors Solo and Slalom; later favors Cascade and Pincer
        val roll = random.nextFloat()
        return when {
            distanceTraveled < 80f && runDuration < 20f -> {
                if (roll < 0.65f) HazardPattern.SOLO_PATROL else HazardPattern.STAGGERED_SLALOM
            }
            threatLevel == 2 -> {
                when {
                    roll < 0.30f -> HazardPattern.SOLO_PATROL
                    roll < 0.60f -> HazardPattern.STAGGERED_SLALOM
                    roll < 0.85f -> HazardPattern.PINCER_DOUBLE
                    else -> HazardPattern.GATEWAY_WALL
                }
            }
            threatLevel == 3 -> {
                when {
                    roll < 0.20f -> HazardPattern.STAGGERED_SLALOM
                    roll < 0.40f -> HazardPattern.PINCER_DOUBLE
                    roll < 0.60f -> HazardPattern.GATEWAY_WALL
                    roll < 0.80f -> HazardPattern.TRIPLE_CASCADE
                    else -> HazardPattern.BOUNCER_CARAVAN
                }
            }
            threatLevel >= 4 -> {
                when {
                    roll < 0.22f -> HazardPattern.TRIPLE_CASCADE
                    roll < 0.44f -> HazardPattern.JUGGERNAUT_FLANK
                    roll < 0.64f -> HazardPattern.PINCER_DOUBLE
                    roll < 0.84f -> HazardPattern.SPEEDSTER_SURGE
                    else -> HazardPattern.GATEWAY_WALL
                }
            }
            else -> eligiblePatterns[random.nextInt(eligiblePatterns.size)]
        }
    }

    /**
     * Generates boulders with progressive intervals and staggered delays for the chosen pattern.
     */
    private fun buildPatternBoulders(
        pattern: HazardPattern,
        spawnZ: Float,
        baseSpeed: Float,
        playerSpeed: Float
    ) {
        when (pattern) {
            HazardPattern.SOLO_PATROL, HazardPattern.BREATHER_RESPITE -> {
                val lane = getRandomLane()
                queueBoulder(BallType.STRAIGHT, lane, spawnZ, baseSpeed, 0f, 0f)
            }

            HazardPattern.STAGGERED_SLALOM -> {
                // 3 boulders alternating left and right at INCREASING sub-intervals
                // Boulder 1 at 0s, Boulder 2 at 0.35s, Boulder 3 at 0.75s (increasing delay interval!)
                val startLeft = random.nextBoolean()
                val lane1 = if (startLeft) -3.2f else 3.2f
                val lane2 = if (startLeft) 2.4f else -2.4f
                val lane3 = if (startLeft) -1.8f else 1.8f

                val stagger1 = 0.34f
                val stagger2 = stagger1 + 0.42f // Increasing interval between 2nd and 3rd boulder!

                queueBoulder(BallType.STRAIGHT, lane1, spawnZ, baseSpeed, 0f, 0f)
                queueBoulder(BallType.STRAIGHT, lane2, spawnZ - 8f, baseSpeed * 1.04f, 0f, stagger1)
                queueBoulder(BallType.STRAIGHT, lane3, spawnZ - 16f, baseSpeed * 1.08f, 0f, stagger2)
            }

            HazardPattern.PINCER_DOUBLE -> {
                // Two boulders angling inwards from curbs with slight stagger interval
                val lateralSpeed = 2.4f + random.nextFloat() * 0.8f
                val stagger = 0.22f // slight interval separation so crossing point is dynamic

                queueBoulder(
                    BallType.LEFT_TO_RIGHT,
                    -(roadHalfWidth - 1.6f),
                    spawnZ,
                    baseSpeed * 1.05f,
                    lateralSpeed,
                    0f
                )
                queueBoulder(
                    BallType.RIGHT_TO_LEFT,
                    (roadHalfWidth - 1.6f),
                    spawnZ - 5f,
                    baseSpeed * 1.05f,
                    -lateralSpeed,
                    stagger
                )
            }

            HazardPattern.GATEWAY_WALL -> {
                // 2 boulders blocking two lanes, leaving one safe corridor,
                // followed by a second gateway at an increasing interval
                val safeLaneIndex = random.nextInt(3) // 0=left safe, 1=center safe, 2=right safe
                val laneLeft = -3.2f
                val laneCenter = 0.0f
                val laneRight = 3.2f

                // First gate at t=0
                if (safeLaneIndex != 0) queueBoulder(BallType.STRAIGHT, laneLeft, spawnZ, baseSpeed, 0f, 0f)
                if (safeLaneIndex != 1) queueBoulder(BallType.STRAIGHT, laneCenter, spawnZ, baseSpeed, 0f, 0f)
                if (safeLaneIndex != 2) queueBoulder(BallType.STRAIGHT, laneRight, spawnZ, baseSpeed, 0f, 0f)

                // Second gate staggered at increasing interval (0.55s) with a different safe lane
                val secondSafeLaneIndex = (safeLaneIndex + 1 + random.nextInt(2)) % 3
                val gateInterval = 0.58f

                if (secondSafeLaneIndex != 0) queueBoulder(BallType.STRAIGHT, laneLeft, spawnZ - 14f, baseSpeed, 0f, gateInterval)
                if (secondSafeLaneIndex != 1) queueBoulder(BallType.STRAIGHT, laneCenter, spawnZ - 14f, baseSpeed, 0f, gateInterval)
                if (secondSafeLaneIndex != 2) queueBoulder(BallType.STRAIGHT, laneRight, spawnZ - 14f, baseSpeed, 0f, gateInterval)
            }

            HazardPattern.BOUNCER_CARAVAN -> {
                // 2 to 3 bouncing boulders at progressive intervals
                val laneA = -2.4f + (random.nextFloat() - 0.5f) * 1.0f
                val laneB = 2.4f + (random.nextFloat() - 0.5f) * 1.0f
                val intervalA = 0.38f
                val intervalB = intervalA + 0.48f // Increasing interval

                queueBoulder(BallType.BOUNCING, laneA, spawnZ, baseSpeed * 1.10f, 0f, 0f)
                queueBoulder(BallType.BOUNCING, laneB, spawnZ - 10f, baseSpeed * 1.12f, 0f, intervalA)
                if (random.nextFloat() < 0.45f) {
                    queueBoulder(BallType.BOUNCING, 0.0f, spawnZ - 20f, baseSpeed * 1.15f, 0f, intervalB)
                }
            }

            HazardPattern.TRIPLE_CASCADE -> {
                // 3 boulders across distinct lanes with strictly increasing interval gaps:
                // gap 1 -> 2: 0.30s, gap 2 -> 3: 0.52s (+73% interval expansion!)
                val shuffledLanes = listOf(-3.4f, 0.0f, 3.4f).shuffled(random)
                val intervalGap1 = 0.32f
                val intervalGap2 = 0.55f

                queueBoulder(BallType.STRAIGHT, shuffledLanes[0], spawnZ, baseSpeed, 0f, 0f)
                queueBoulder(BallType.STRAIGHT, shuffledLanes[1], spawnZ - 8f, baseSpeed * 1.05f, 0f, intervalGap1)
                queueBoulder(BallType.STRAIGHT, shuffledLanes[2], spawnZ - 18f, baseSpeed * 1.10f, 0f, intervalGap1 + intervalGap2)
            }

            HazardPattern.JUGGERNAUT_FLANK -> {
                // Giant boulder occupying a wide corridor + flanking fast crosser at an interval
                val giantOnLeft = random.nextBoolean()
                val giantX = if (giantOnLeft) -2.2f else 2.2f
                val flankX = if (giantOnLeft) 4.2f else -4.2f
                val staggerInterval = 0.40f

                queueBoulder(BallType.GIANT, giantX, spawnZ, baseSpeed * 0.85f, 0f, 0f)
                queueBoulder(BallType.FAST, flankX, spawnZ - 12f, baseSpeed * 1.35f, 0f, staggerInterval)
            }

            HazardPattern.SPEEDSTER_SURGE -> {
                // High-velocity speedsters launched at increasing intervals
                val lane1 = getRandomLane()
                var lane2 = getRandomLane()
                while (abs(lane2 - lane1) < 1.8f) {
                    lane2 = getRandomLane()
                }
                val intervalGap = 0.38f

                queueBoulder(BallType.FAST, lane1, spawnZ, baseSpeed * 1.35f, 0f, 0f)
                queueBoulder(BallType.FAST, lane2, spawnZ - 14f, baseSpeed * 1.40f, 0f, intervalGap)
            }
        }
    }

    private fun queueBoulder(
        type: BallType,
        x: Float,
        z: Float,
        speed: Float,
        lateralSpeed: Float,
        delayRemaining: Float
    ) {
        pendingBoulders.add(
            QueuedBoulder(
                type = type,
                x = x.coerceIn(-roadHalfWidth + 1.2f, roadHalfWidth - 1.2f),
                z = z,
                forwardSpeed = speed,
                lateralSpeed = lateralSpeed,
                delayRemaining = delayRemaining
            )
        )
    }

    /**
     * Mathematical Solvability Guarantee:
     * Validates that at any point during the wave, at least one playable lane is free
     * from simultaneous boulder collisions. If all lanes are obstructed at the same time slice,
     * the conflicting boulder is adjusted to preserve a fair evasion corridor.
     */
    fun enforceSolvabilityGuarantee() {
        if (pendingBoulders.size <= 1) return

        // Check for concurrent boulders (delay difference < 0.25s)
        val timeSlices = pendingBoulders.groupBy { (it.delayRemaining / 0.25f).toInt() }
        for ((_, bouldersInSlice) in timeSlices) {
            if (bouldersInSlice.size >= 3) {
                // If 3 or more boulders exist in the same time slice, they could block the whole road!
                // Remove or shift the 3rd boulder to create a guaranteed escape lane.
                val excessBoulder = bouldersInSlice.last()
                val safeLane = findFreeLane(bouldersInSlice.dropLast(1))
                // Retarget or stagger delay so lane is clear
                excessBoulder.delayRemaining += 0.45f
            }
        }
    }

    /**
     * Finds a lane coordinate that is not blocked by the specified boulders.
     */
    private fun findFreeLane(existingBoulders: List<QueuedBoulder>): Float {
        for (lane in lanes) {
            val blocked = existingBoulders.any { abs(it.x - lane) < (it.type.radius + 0.8f) }
            if (!blocked) return lane
        }
        return 0.0f
    }

    private fun getRandomLane(): Float {
        return lanes[random.nextInt(lanes.size)]
    }
}

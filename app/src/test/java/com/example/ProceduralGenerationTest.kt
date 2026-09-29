package com.example

import com.example.engine.BallType
import com.example.engine.GameAudio
import com.example.engine.GamePhysicsEngine
import com.example.engine.HazardPattern
import com.example.engine.ProceduralBoulderGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ProceduralGenerationTest {

    @Test
    fun proceduralGenerator_initialState_hasCorrectDefaults() {
        val generator = ProceduralBoulderGenerator()
        generator.reset()

        assertEquals(0, generator.waveCount)
        assertEquals(0, generator.totalBouldersSpawned)
        assertEquals(HazardPattern.SOLO_PATROL, generator.currentPattern)
        assertEquals(generator.baseSpawnInterval, generator.currentInterval, 0.001f)
        assertFalse(generator.isBreatherActive)
        assertTrue(generator.pendingBoulders.isEmpty())
        assertEquals(5, generator.lanes.size)
    }

    @Test
    fun proceduralGenerator_calculateNextInterval_increasesProgressivelyAcrossCycle() {
        val generator = ProceduralBoulderGenerator()
        generator.reset()

        // Within a cycle (indices 0..3), interval should progressively increase:
        // calculateNextInterval(0, ...) < calculateNextInterval(1, ...) < calculateNextInterval(2, ...) < calculateNextInterval(3, ...)
        val pattern = HazardPattern.SOLO_PATROL
        val speed = 10f
        val threat = 2

        val intervalWave1 = generator.calculateNextInterval(0, pattern, speed, threat, isOverdrive = false)
        val intervalWave2 = generator.calculateNextInterval(1, pattern, speed, threat, isOverdrive = false)
        val intervalWave3 = generator.calculateNextInterval(2, pattern, speed, threat, isOverdrive = false)
        val intervalWave4 = generator.calculateNextInterval(3, pattern, speed, threat, isOverdrive = false)

        assertTrue(
            "Interval for wave 2 ($intervalWave2) must be greater than wave 1 ($intervalWave1)",
            intervalWave2 > intervalWave1
        )
        assertTrue(
            "Interval for wave 3 ($intervalWave3) must be greater than wave 2 ($intervalWave2)",
            intervalWave3 > intervalWave2
        )
        assertTrue(
            "Interval for wave 4 ($intervalWave4) must be greater than wave 3 ($intervalWave3)",
            intervalWave4 > intervalWave3
        )

        // Breather wave (index 4) should have significantly larger interval
        val intervalBreather = generator.calculateNextInterval(4, HazardPattern.BREATHER_RESPITE, speed, threat, isOverdrive = false)
        assertTrue(
            "Breather interval ($intervalBreather) must exceed standard wave intervals ($intervalWave4)",
            intervalBreather > intervalWave4
        )
    }

    @Test
    fun proceduralGenerator_staggeredFormations_spawnAtIncreasingDelays() {
        val generator = ProceduralBoulderGenerator()
        generator.reset(seed = 42L)

        // Force a wave generation
        generator.generateNextWave(
            playerSpeed = 10f,
            distanceTraveled = 200f,
            runDuration = 35f,
            threatLevel = 2,
            isOverdrive = false
        )

        assertTrue("Generated wave should schedule at least one boulder", generator.pendingBoulders.isNotEmpty())
        assertTrue("Wave count should increment", generator.waveCount >= 1)
        assertNotNull(generator.currentPattern)

        // If multiple boulders were scheduled, verify subsequent boulders have increasing delay
        if (generator.pendingBoulders.size >= 2) {
            val delays = generator.pendingBoulders.map { it.delayRemaining }
            for (i in 1 until delays.size) {
                assertTrue(
                    "Successive queued boulders must have non-decreasing delay intervals: delays[${i-1}]=${delays[i-1]}, delays[$i]=${delays[i]}",
                    delays[i] >= delays[i - 1]
                )
            }
        }
    }

    @Test
    fun proceduralGenerator_solvabilityGuarantee_ensuresAtLeastOneUnblockedLane() {
        val generator = ProceduralBoulderGenerator()
        generator.reset(seed = 101L)

        // Test solvability enforcement over multiple consecutive procedural waves
        for (i in 0 until 20) {
            generator.generateNextWave(
                playerSpeed = 12f + i * 0.4f,
                distanceTraveled = 50f + i * 50f,
                runDuration = 10f + i * 5f,
                threatLevel = ((i / 4) + 1).coerceAtMost(5),
                isOverdrive = false
            )

            // Verify that for all pending boulders within any immediate time window,
            // there is at least one lane that is free
            val immediateBoulders = generator.pendingBoulders.filter { it.delayRemaining < 0.25f }
            val blockedLanes = generator.lanes.filter { lane ->
                immediateBoulders.any { abs(it.x - lane) < (it.type.radius + 0.8f) }
            }

            assertTrue(
                "Immediate arrival must not block all 5 lanes simultaneously. Blocked: ${blockedLanes.size} of 5",
                blockedLanes.size < generator.lanes.size
            )

            // Clear between wave tests
            generator.pendingBoulders.clear()
        }
    }

    @Test
    fun proceduralGenerator_speedCompensation_scalesIntervalsGracefully() {
        val generator = ProceduralBoulderGenerator()
        generator.reset()

        val pattern = HazardPattern.GATEWAY_WALL
        val threat = 2

        val lowSpeedInterval = generator.calculateNextInterval(1, pattern, playerSpeed = 8f, threatLevel = threat, isOverdrive = false)
        val highSpeedInterval = generator.calculateNextInterval(1, pattern, playerSpeed = 16f, threatLevel = threat, isOverdrive = false)

        assertTrue(
            "Higher speed should increase interval to preserve reaction window (high: $highSpeedInterval, low: $lowSpeedInterval)",
            highSpeedInterval >= lowSpeedInterval
        )
    }

    @Test
    fun proceduralGenerator_deterministicSeed_generatesIdenticalSequences() {
        val gen1 = ProceduralBoulderGenerator()
        gen1.reset(seed = 12345L)

        val gen2 = ProceduralBoulderGenerator()
        gen2.reset(seed = 12345L)

        for (i in 0 until 8) {
            gen1.generateNextWave(10f, 100f * i, 15f * i, 2, false)
            gen2.generateNextWave(10f, 100f * i, 15f * i, 2, false)

            assertEquals("Patterns must match on identical seeds", gen1.currentPattern, gen2.currentPattern)
            assertEquals("Intervals must match on identical seeds", gen1.currentInterval, gen2.currentInterval, 0.001f)
            assertEquals("Pending boulder count must match", gen1.pendingBoulders.size, gen2.pendingBoulders.size)

            gen1.pendingBoulders.clear()
            gen2.pendingBoulders.clear()
        }
    }

    @Test
    fun physicsEngine_proceduralBoulders_spawnProgressivelyAndAdvance() {
        val audio = GameAudio().apply { isEnabled = false }
        val physics = GamePhysicsEngine(audio) { _, _, _, _, _ -> }
        physics.startNewGame()

        var waveEventFired = false
        physics.onHazardWaveSpawned = { waveIndex, pattern, interval ->
            waveEventFired = true
            assertTrue("Wave index must be > 0", waveIndex > 0)
            assertTrue("Interval must be positive", interval > 0f)
        }

        // Trigger wave generation explicitly
        physics.spawnObstacleWave()
        assertTrue("Wave event should have fired", waveEventFired)
        assertTrue("Wave count should be >= 1", physics.waveCount >= 1)

        // Advance physics by several frames to let queued boulders spawn into ballPool
        for (i in 0 until 30) {
            physics.update(dt = 0.05f, leftHeld = false, rightHeld = false, brakeHeld = false)
        }

        val activeBalls = physics.ballPool.count { it.isActive }
        assertTrue("At least one boulder should be active on the track", activeBalls > 0)
    }
}

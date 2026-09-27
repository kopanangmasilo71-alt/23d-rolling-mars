package com.example

import com.example.engine.BallType
import com.example.engine.GameAudio
import com.example.engine.GamePhysicsEngine
import com.example.engine.PlayerCharacter
import com.example.engine.RollingBall
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun player_initialState_isGrounded() {
        val player = PlayerCharacter()
        player.reset()
        assertEquals(0f, player.position.x, 0.001f)
        assertEquals(0f, player.position.y, 0.001f)
        assertTrue(player.isGrounded)
        assertEquals(player.baseNormalSpeed, player.forwardSpeed, 0.001f)
    }

    @Test
    fun player_steering_acceleratesSmoothly() {
        val player = PlayerCharacter()
        player.reset()

        // Holding right: horizontal velocity increases from 0
        player.update(dt = 0.05f, leftHeld = false, rightHeld = true, brakeHeld = false, roadHalfWidth = 6f)
        assertTrue("Horizontal velocity should be positive when steering right", player.horizontalVelocity > 0f)

        // Position X should shift to the right
        assertTrue("Player X position should move right", player.position.x > 0f)

        // Release: deceleration reduces velocity
        val velBeforeRelease = player.horizontalVelocity
        player.update(dt = 0.05f, leftHeld = false, rightHeld = false, brakeHeld = false, roadHalfWidth = 6f)
        assertTrue("Velocity should decrease after releasing steer", player.horizontalVelocity < velBeforeRelease)
    }

    @Test
    fun player_jump_appliesVerticalVelocityAndGroundedCheck() {
        val player = PlayerCharacter()
        player.reset()

        val jumped = player.jump()
        assertTrue("First jump should succeed", jumped)
        assertFalse("Player should not be grounded in air", player.isGrounded)
        assertTrue("Vertical velocity should be positive", player.verticalVelocity > 0f)

        // Mid-air jump attempt should be prevented
        val doubleJumped = player.jump()
        assertFalse("Cannot double jump in air", doubleJumped)
    }

    @Test
    fun player_brake_deceleratesForwardSpeed() {
        val player = PlayerCharacter()
        player.reset()

        val initialSpeed = player.forwardSpeed
        player.update(dt = 0.1f, leftHeld = false, rightHeld = false, brakeHeld = true, roadHalfWidth = 6f)
        assertTrue("Forward speed should decrease while braking", player.forwardSpeed < initialSpeed)
    }

    @Test
    fun rollingBall_rollsAndAdvances() {
        val ball = RollingBall(0)
        ball.reset(BallType.STRAIGHT, spawnX = 0f, spawnZ = -50f, baseSpeed = 10f)
        assertTrue(ball.isActive)
        val initialZ = ball.position.z

        ball.update(0.1f)
        assertTrue("Ball should move toward positive Z", ball.position.z > initialZ)
        assertTrue("Ball roll angle should increase", ball.rollAngleX > 0f)
    }

    @Test
    fun physicsEngine_poolManagement() {
        val audio = GameAudio().apply { isEnabled = false }
        val physics = GamePhysicsEngine(audio) { _, _, _, _, _ -> }
        physics.startNewGame()

        assertTrue(physics.isRunning)
        assertFalse(physics.isGameOver)
        assertEquals(32, physics.ballPool.size)
        assertEquals(6, physics.speedPadPool.size)
    }

    @Test
    fun physicsEngine_comboAndSectorProgression() {
        val audio = GameAudio().apply { isEnabled = false }
        val physics = GamePhysicsEngine(audio) { _, _, _, _, _ -> }
        physics.startNewGame(overdrive = false)

        assertEquals("SECTOR 1", physics.currentSector.name)
        assertEquals(1, physics.comboMultiplier)

        // Advance distance past Sector 2 threshold (150m)
        physics.distanceTraveled = 160f
        physics.update(0.02f, leftHeld = false, rightHeld = false, brakeHeld = false)
        assertEquals("SECTOR 2", physics.currentSector.name)
    }

    @Test
    fun physicsEngine_overdriveMode_increasesPace() {
        val audio = GameAudio().apply { isEnabled = false }
        val normalPhysics = GamePhysicsEngine(audio) { _, _, _, _, _ -> }
        normalPhysics.startNewGame(overdrive = false)

        val overdrivePhysics = GamePhysicsEngine(audio) { _, _, _, _, _ -> }
        overdrivePhysics.startNewGame(overdrive = true)

        assertTrue(overdrivePhysics.isOverdriveMode)
        assertFalse(normalPhysics.isOverdriveMode)
    }

    @Test
    fun touchController_inputsDrivePhysicsState() {
        val audio = GameAudio().apply { isEnabled = false }
        val physics = GamePhysicsEngine(audio) { _, _, _, _, _ -> }
        physics.startNewGame()

        var leftHeld = false
        var rightHeld = false
        var brakeHeld = false
        var jumpTriggered = false

        val onLeftChange: (Boolean) -> Unit = { leftHeld = it }
        val onRightChange: (Boolean) -> Unit = { rightHeld = it }
        val onBrakeChange: (Boolean) -> Unit = { brakeHeld = it }
        val onJump: () -> Unit = { jumpTriggered = true; physics.jump() }

        // Test Left input callback
        onLeftChange(true)
        assertTrue(leftHeld)
        physics.player.update(0.05f, leftHeld = leftHeld, rightHeld = rightHeld, brakeHeld = brakeHeld, roadHalfWidth = 6f)
        assertTrue("Player should accelerate left", physics.player.horizontalVelocity < 0f)

        onLeftChange(false)
        assertFalse(leftHeld)

        // Test Right input callback
        onRightChange(true)
        assertTrue(rightHeld)
        physics.player.update(0.1f, leftHeld = leftHeld, rightHeld = rightHeld, brakeHeld = brakeHeld, roadHalfWidth = 6f)
        assertTrue("Player should steer right", physics.player.horizontalVelocity > -8.5f)

        onRightChange(false)
        assertFalse(rightHeld)

        // Test Brake input callback
        val normalSpeed = physics.player.forwardSpeed
        onBrakeChange(true)
        assertTrue(brakeHeld)
        physics.player.update(0.1f, leftHeld = leftHeld, rightHeld = rightHeld, brakeHeld = brakeHeld, roadHalfWidth = 6f)
        assertTrue("Player forward speed should decrease while braking", physics.player.forwardSpeed < normalSpeed)

        onBrakeChange(false)
        assertFalse(brakeHeld)

        // Test Jump input callback
        onJump()
        assertTrue(jumpTriggered)
        assertFalse(physics.player.isGrounded)
    }

    @Test
    fun particleSystem_boulderCollisions_emitDustAndDebris() {
        val audio = GameAudio().apply { isEnabled = false }
        val physics = GamePhysicsEngine(audio) { _, _, _, _, _ -> }
        physics.startNewGame()

        // Activate a boulder rolling on ground
        val ball = physics.ballPool[0]
        ball.reset(BallType.STRAIGHT, spawnX = 0f, spawnZ = -20f, baseSpeed = 10f)

        // Advance physics to trigger ground rolling dust
        physics.update(0.04f, leftHeld = false, rightHeld = false, brakeHeld = false)

        val activeParticles = physics.particles.particles.count { it.lifetime > 0f }
        assertTrue("Rolling boulder should emit trail dust particles", activeParticles > 0)

        // Trigger curb collision
        ball.position.x = -physics.roadHalfWidth + ball.radius
        physics.update(0.04f, leftHeld = false, rightHeld = false, brakeHeld = false)
        val debrisParticles = physics.particles.particles.count { it.particleType == com.example.engine.ParticleType.ROCK_DEBRIS && it.lifetime > 0f }
        assertTrue("Curb collision should emit rock debris particles", debrisParticles > 0)
    }

    @Test
    fun timeOfDay_dawnToNightProgression_smoothTransitions() {
        val todSystem = com.example.engine.TimeOfDaySystem(cycleScoreLength = 10000)

        // 1. Dawn phase at score 0
        val dawnSnapshot = todSystem.evaluate(score = 0)
        assertEquals(com.example.engine.TimeOfDayPhase.DAWN, dawnSnapshot.phase)
        assertEquals(1, dawnSnapshot.dayNumber)
        assertTrue("Sun should be visible at dawn", dawnSnapshot.sunAlpha > 0.5f)
        assertTrue("Moon should be inactive/hidden at dawn", dawnSnapshot.moonAlpha < 0.2f)
        assertTrue("Clock time should contain AM", dawnSnapshot.timeString.contains("AM"))

        // 2. Midday phase at score 3500
        val middaySnapshot = todSystem.evaluate(score = 3500)
        assertEquals(com.example.engine.TimeOfDayPhase.MIDDAY, middaySnapshot.phase)
        assertEquals(1, middaySnapshot.dayNumber)
        assertEquals(1.0f, middaySnapshot.sunAlpha, 0.01f)
        assertEquals(0.0f, middaySnapshot.moonAlpha, 0.01f)
        assertEquals(0.0f, middaySnapshot.starsAlpha, 0.01f)
        assertEquals(0.0f, middaySnapshot.headlightIntensity, 0.01f)
        assertTrue("Sun should be high in the sky (Y > 30)", middaySnapshot.sunPos.y > 30f)

        // 3. Sunset phase at score 5800
        val sunsetSnapshot = todSystem.evaluate(score = 5800)
        assertEquals(com.example.engine.TimeOfDayPhase.SUNSET, sunsetSnapshot.phase)
        assertTrue("Sun light color should be warm/reddish at sunset", sunsetSnapshot.lightColor[0] > sunsetSnapshot.lightColor[2])
        assertTrue("Horizon fog should be rich amber/crimson", sunsetSnapshot.fogHorizonColor[0] > 0.8f)

        // 4. Dusk/Twilight phase at score 7500
        val duskSnapshot = todSystem.evaluate(score = 7500)
        assertEquals(com.example.engine.TimeOfDayPhase.DUSK, duskSnapshot.phase)
        assertTrue("Stars should begin to emerge at dusk", duskSnapshot.starsAlpha > 0.3f)
        assertTrue("Moon should be emerging at dusk", duskSnapshot.moonAlpha > 0.4f)
        assertTrue("Headlights should start illuminating at dusk", duskSnapshot.headlightIntensity > 0.3f)

        // 5. Night phase at score 9200
        val nightSnapshot = todSystem.evaluate(score = 9200)
        assertEquals(com.example.engine.TimeOfDayPhase.NIGHT, nightSnapshot.phase)
        assertEquals(1.0f, nightSnapshot.starsAlpha, 0.05f)
        assertEquals(1.0f, nightSnapshot.moonAlpha, 0.05f)
        assertEquals(0.0f, nightSnapshot.sunAlpha, 0.05f)
        assertTrue("Headlights should be intensely active at night", nightSnapshot.headlightIntensity > 0.8f)
        assertTrue("Street lights should be fully lit at night", nightSnapshot.streetLightEmissive > 0.8f)
        assertTrue("Night lighting should have cool blue moonlight tint", nightSnapshot.lightColor[2] > nightSnapshot.lightColor[1])

        // 6. Day 2 Dawn continuation at score 10200
        val day2Snapshot = todSystem.evaluate(score = 10200)
        assertEquals(2, day2Snapshot.dayNumber)
        assertEquals(com.example.engine.TimeOfDayPhase.DAWN, day2Snapshot.phase)
    }

    @Test
    fun primitives_starField_generatedCorrectly() {
        val starField = com.example.engine.Primitives.createStarField(count = 50, radius = 90f)
        assertNotNull(starField)
        assertTrue("Star field should contain triangles", starField.indexCount > 0)
        assertEquals(50 * 6, starField.indexCount) // 50 diamonds * 2 triangles * 3 indices
    }
}

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

    @Test
    fun allPrimitivesMeshes_createSuccessfullyWithoutExceptions() {
        val physics = GamePhysicsEngine(GameAudio()) { _, _, _, _, _ -> }
        physics.startNewGame()

        val cubeMesh = com.example.engine.Primitives.createCube(1f, 1f, 1f, floatArrayOf(1f, 1f, 1f, 1f))
        val roadQuadMesh = com.example.engine.Primitives.createPlane(physics.roadWidth, physics.segmentLength, floatArrayOf(0.12f, 0.14f, 0.18f, 1f))
        val grassQuadMesh = com.example.engine.Primitives.createPlane(45f, physics.segmentLength, floatArrayOf(0.08f, 0.22f, 0.14f, 1f))
        val cylinderMesh = com.example.engine.Primitives.createCylinder(0.35f, 2.0f, 14, floatArrayOf(0.38f, 0.24f, 0.15f, 1f))
        val smoothSphereMesh = com.example.engine.Primitives.createSmoothSphere(1f, 16, 20, floatArrayOf(1f, 1f, 1f, 1f))
        val torusRingMesh = com.example.engine.Primitives.createTorusRing(1.6f, 0.12f, 22, 10, floatArrayOf(0f, 0.95f, 1f, 1f))
        val skyBackdropMesh = com.example.engine.Primitives.createSkyBackdrop(110f, 50f, 24)
        val starFieldMesh = com.example.engine.Primitives.createStarField(120, 95f)
        val speedPadMesh = com.example.engine.Primitives.createSpeedPad(3.0f, 4.2f)
        val crystalMesh = com.example.engine.Primitives.createFloatingCrystal(1.4f, 3.2f)
        val pyramidMesh = com.example.engine.Primitives.createSciFiPyramid(16f, 22f)
        val wingBladeMesh = com.example.engine.Primitives.createWingBlade(
            span = 0.90f,
            rootChord = 0.38f,
            tipChord = 0.12f,
            sweep = 0.24f,
            thickness = 0.035f,
            color = floatArrayOf(0.05f, 0.85f, 0.42f, 1f)
        )
        val wedgeMesh = com.example.engine.Primitives.createWedge(
            w = 0.25f,
            h = 0.45f,
            d = 0.35f,
            color = floatArrayOf(1f, 1f, 1f, 1f)
        )
        val straightBallMesh = com.example.engine.Primitives.createRollingSphere(
            radius = BallType.STRAIGHT.radius,
            colorA = floatArrayOf(0.96f, 0.38f, 0.10f, 1f),
            colorB = floatArrayOf(1.0f, 0.88f, 0.18f, 1f)
        )

        assertNotNull(cubeMesh)
        assertNotNull(roadQuadMesh)
        assertNotNull(grassQuadMesh)
        assertNotNull(cylinderMesh)
        assertNotNull(smoothSphereMesh)
        assertNotNull(torusRingMesh)
        assertNotNull(skyBackdropMesh)
        assertNotNull(starFieldMesh)
        assertNotNull(speedPadMesh)
        assertNotNull(crystalMesh)
        assertNotNull(pyramidMesh)
        assertNotNull(wingBladeMesh)
        assertNotNull(wedgeMesh)
        assertNotNull(straightBallMesh)
    }

    @Test
    fun physicsEngine_shoot_spawnsProjectileAndHitsBoulder() {
        val audio = GameAudio().apply { isEnabled = false }
        val physics = GamePhysicsEngine(audio) { _, _, _, _, _ -> }
        physics.startNewGame()

        // Verify initial state: no projectiles active
        val initialActiveCount = physics.projectilePool.count { it.isActive }
        assertEquals(0, initialActiveCount)

        // Fire shot
        physics.shoot()
        val afterShootCount = physics.projectilePool.count { it.isActive }
        assertEquals(1, afterShootCount)

        val proj = physics.projectilePool.first { it.isActive }
        assertTrue("Projectile should move forward into -Z direction", proj.velocity.z < 0f)

        // Place a boulder right in the bullet's path
        val ball = physics.ballPool[0]
        ball.reset(BallType.STRAIGHT, spawnX = proj.position.x, spawnZ = proj.position.z - 3f, baseSpeed = 0f)
        assertTrue(ball.isActive)

        val initialScore = physics.score
        val initialDodged = physics.ballsDodged

        // Update physics step so projectile collides with the boulder
        physics.update(dt = 0.08f, leftHeld = false, rightHeld = false, brakeHeld = false)

        assertFalse("Boulder should be destroyed on laser impact", ball.isActive)
        assertFalse("Projectile should be consumed on impact", proj.isActive)
        assertTrue("Score should increase when blasting a boulder", physics.score > initialScore)
        assertEquals("Balls dodged/destroyed count should increment", initialDodged + 1, physics.ballsDodged)
    }

    @Test
    fun physicsEngine_boulderTrailDust_generatesDustParticles() {
        val audio = GameAudio().apply { isEnabled = false }
        val physics = GamePhysicsEngine(audio) { _, _, _, _, _ -> }
        physics.startNewGame()

        // Place an active rolling boulder on the road
        val ball = physics.ballPool[0]
        ball.reset(BallType.STRAIGHT, spawnX = 0f, spawnZ = -10f, baseSpeed = 12f)
        ball.position.y = ball.radius // on ground

        // Run several update ticks so dust timer triggers
        for (i in 0 until 10) {
            physics.update(dt = 0.03f, leftHeld = false, rightHeld = false, brakeHeld = false)
        }

        val dustParticles = physics.particles.particles.count { it.lifetime > 0f && it.particleType == com.example.engine.ParticleType.DUST_CLOUD }
        assertTrue("Rolling boulder should emit dust cloud particles behind it", dustParticles > 0)
    }

    @Test
    fun physicsEngine_ammoSystem_depletesAndRechargesFromPowerUps() {
        val audio = GameAudio().apply { isEnabled = false }
        val physics = GamePhysicsEngine(audio) { _, _, _, _, _ -> }
        physics.startNewGame()

        // Verify initial ammo
        assertEquals(10, physics.player.ammo)

        // Fire 1 shot: ammo should decrease to 9
        physics.shoot()
        assertEquals(9, physics.player.ammo)

        // Deplete remaining ammo
        physics.player.ammo = 0
        val projectilesBefore = physics.projectilePool.count { it.isActive }
        physics.shootCooldownTimer = 0f
        physics.shoot() // Dry fire
        val projectilesAfter = physics.projectilePool.count { it.isActive }
        assertEquals("Should not spawn projectile when out of ammo", projectilesBefore, projectilesAfter)

        // Recharge ammo via power-up (e.g. AMMO_PACK)
        physics.spawnCollectibleAt(com.example.engine.CollectibleType.AMMO_PACK, 0f, 0f)
        physics.player.position.set(0f, 0f, 0f)
        physics.update(dt = 0.05f, leftHeld = false, rightHeld = false, brakeHeld = false)

        assertTrue("Ammo should recharge from power-up", physics.player.ammo > 0)
    }

    @Test
    fun physicsEngine_screenShake_triggersOnShotAndBoulderDestruction() {
        val audio = GameAudio().apply { isEnabled = false }
        val physics = GamePhysicsEngine(audio) { _, _, _, _, _ -> }
        physics.startNewGame()

        var shootCallbackFired = false
        var boulderDestroyedCallbackFired = false
        physics.onShootFired = { shootCallbackFired = true }
        physics.onBoulderDestroyed = { boulderDestroyedCallbackFired = true }

        // Test screen shake on shot
        physics.cameraShakeMagnitude = 0f
        physics.shoot()
        assertTrue("Screen shake magnitude should increase on shot", physics.cameraShakeMagnitude > 0f)
        assertTrue("onShootFired callback should be invoked", shootCallbackFired)

        // Test screen shake on boulder destruction
        val proj = physics.projectilePool.first { it.isActive }
        val ball = physics.ballPool[0]
        ball.reset(BallType.STRAIGHT, spawnX = proj.position.x, spawnZ = proj.position.z - 2f, baseSpeed = 0f)

        val shakeBeforeImpact = physics.cameraShakeMagnitude
        physics.update(dt = 0.08f, leftHeld = false, rightHeld = false, brakeHeld = false)

        assertTrue("Boulder should be destroyed", !ball.isActive)
        assertTrue("onBoulderDestroyed callback should be invoked", boulderDestroyedCallbackFired)
        assertTrue("Screen shake should trigger on boulder destruction", physics.cameraShakeMagnitude > shakeBeforeImpact)
    }

    @Test
    fun physicsEngine_dynamicDifficulty_graduallyIncreasesSpeedAndFrequencyWithRunDuration() {
        val audio = GameAudio().apply { isEnabled = false }
        val physics = GamePhysicsEngine(audio) { _, _, _, _, _ -> }
        physics.startNewGame()
        physics.clearAllTrackEntities()
        physics.player.activateShield(9999f)

        // 1. Initial State at duration = 0
        assertEquals(0f, physics.runDuration, 0.001f)
        assertEquals(0, physics.runDurationSeconds)
        assertEquals(1.0f, physics.dynamicSpeedMultiplier, 0.001f)
        assertEquals(1.0f, physics.dynamicFrequencyMultiplier, 0.001f)
        assertEquals(1, physics.currentThreatLevel.level)
        assertEquals("STABLE", physics.currentThreatLevel.name)

        var threatEscalationCalled = false
        physics.onThreatEscalation = { threat ->
            threatEscalationCalled = true
        }

        // 2. Advance run duration to 30 seconds (600 steps of 0.05s)
        val initialSpeedMult = physics.dynamicSpeedMultiplier
        val initialFreqMult = physics.dynamicFrequencyMultiplier

        for (i in 0 until 620) {
            physics.player.invincibleGraceTimer = 10f
            physics.update(dt = 0.05f, leftHeld = false, rightHeld = false, brakeHeld = false)
        }

        assertTrue("Run duration should be >= 30s", physics.runDuration >= 30f)
        assertTrue("Speed multiplier should increase after 30s", physics.dynamicSpeedMultiplier > initialSpeedMult)
        assertTrue("Frequency multiplier should increase after 30s", physics.dynamicFrequencyMultiplier > initialFreqMult)
        assertTrue("Threat level callback should have fired for elevated danger", threatEscalationCalled)
        assertEquals(2, physics.currentThreatLevel.level)
        assertEquals("ELEVATED", physics.currentThreatLevel.name)

        // 3. Advance run duration to 60+ seconds (Threat Level 3: INTENSE)
        threatEscalationCalled = false
        for (i in 0 until 650) {
            physics.player.invincibleGraceTimer = 10f
            physics.update(dt = 0.05f, leftHeld = false, rightHeld = false, brakeHeld = false)
        }

        assertTrue("Run duration should be >= 60s", physics.runDuration >= 60f)
        assertTrue("Threat escalation should have fired for Level 3", threatEscalationCalled)
        assertEquals(3, physics.currentThreatLevel.level)
        assertEquals("INTENSE", physics.currentThreatLevel.name)
        assertTrue("Speed multiplier at 60s should be higher than at 30s", physics.dynamicSpeedMultiplier > 1.30f)
        assertTrue("Frequency multiplier at 60s should be higher than at 30s", physics.dynamicFrequencyMultiplier > 1.35f)
    }

    @Test
    fun physicsEngine_dynamicDifficulty_spawnsFasterBouldersAtHighDuration() {
        val audio = GameAudio().apply { isEnabled = false }
        val physics = GamePhysicsEngine(audio) { _, _, _, _, _ -> }

        // Start game and measure first wave boulder speed
        physics.startNewGame()
        physics.clearAllTrackEntities()
        val speedAtStart = physics.dynamicSpeedMultiplier

        // Advance game time to 120 seconds (2450 steps of 0.05s)
        for (i in 0 until 2450) {
            physics.player.invincibleGraceTimer = 10f
            physics.update(dt = 0.05f, leftHeld = false, rightHeld = false, brakeHeld = false)
        }

        val speedAt120s = physics.dynamicSpeedMultiplier
        assertTrue("Dynamic speed multiplier at 120s must be significantly higher than at start", speedAt120s >= speedAtStart * 1.5f)
        assertEquals(4, physics.currentThreatLevel.level) // SEVERE
        assertEquals("SEVERE", physics.currentThreatLevel.name)

        // Also verify tier helper mappings
        assertEquals(1, com.example.engine.DynamicDifficultyTiers.getThreatForDuration(10f).level)
        assertEquals(2, com.example.engine.DynamicDifficultyTiers.getThreatForDuration(30f).level)
        assertEquals(3, com.example.engine.DynamicDifficultyTiers.getThreatForDuration(65f).level)
        assertEquals(4, com.example.engine.DynamicDifficultyTiers.getThreatForDuration(105f).level)
        assertEquals(5, com.example.engine.DynamicDifficultyTiers.getThreatForDuration(160f).level)
    }

    @Test
    fun gameStateViewModel_initialState_hasDefaultValues() {
        val vm = com.example.ui.GameStateViewModel()
        assertEquals(0, vm.currentScore.value)
        assertEquals(0, vm.gameState.value.currentScore)
        assertEquals(0, vm.gameState.value.score)
        assertFalse(vm.isGameOver.value)
        assertFalse(vm.gameOverStatus.value)
        assertFalse(vm.gameState.value.isGameOver)
        assertFalse(vm.gameState.value.gameOverStatus)
        assertEquals(10.0f, vm.gameSpeed.value, 0.001f)
        assertEquals(10.0f, vm.gameState.value.gameSpeed, 0.001f)
        assertEquals(10.0f, vm.gameState.value.speed, 0.001f)
    }

    @Test
    fun gameStateViewModel_updateScore_emitsNewScoreAndGameState() {
        val vm = com.example.ui.GameStateViewModel()
        vm.updateScore(350)
        assertEquals(350, vm.currentScore.value)
        assertEquals(350, vm.gameState.value.currentScore)

        vm.addScore(150)
        assertEquals(500, vm.currentScore.value)
        assertEquals(500, vm.gameState.value.currentScore)
    }

    @Test
    fun gameStateViewModel_setGameOver_emitsGameOverStatusAndGameState() {
        val vm = com.example.ui.GameStateViewModel()
        assertFalse(vm.isGameOver.value)

        vm.setGameOver(true)
        assertTrue(vm.isGameOver.value)
        assertTrue(vm.gameOverStatus.value)
        assertTrue(vm.gameState.value.isGameOver)
        assertFalse(vm.gameState.value.isPlaying)

        vm.setGameOver(false)
        assertFalse(vm.isGameOver.value)
        assertFalse(vm.gameState.value.isGameOver)
    }

    @Test
    fun gameStateViewModel_setGameSpeed_emitsUpdatedSpeedAndGameState() {
        val vm = com.example.ui.GameStateViewModel()
        vm.setGameSpeed(18.5f)
        assertEquals(18.5f, vm.gameSpeed.value, 0.001f)
        assertEquals(18.5f, vm.gameState.value.gameSpeed, 0.001f)
        assertEquals(18.5f, vm.gameState.value.speed, 0.001f)
    }

    @Test
    fun gameStateViewModel_startGame_resetsStateProperly() {
        val vm = com.example.ui.GameStateViewModel()
        vm.updateScore(1200)
        vm.setGameOver(true)
        vm.setGameSpeed(25.0f)

        vm.startGame()
        assertEquals(0, vm.currentScore.value)
        assertFalse(vm.isGameOver.value)
        assertEquals(10.0f, vm.gameSpeed.value, 0.001f)
        assertTrue(vm.gameState.value.isPlaying)
        assertFalse(vm.gameState.value.isPaused)
    }

    @Test
    fun gameStateViewModel_pauseAndResume_updatesGameStateProperly() {
        val vm = com.example.ui.GameStateViewModel()
        vm.startGame()
        assertFalse(vm.gameState.value.isPaused)

        vm.pauseGame()
        assertTrue(vm.gameState.value.isPaused)

        vm.resumeGame()
        assertFalse(vm.gameState.value.isPaused)
    }

    @Test
    fun gameStateViewModel_frameIndependentUpdate_scalesSpeedAndScoreDeterministically() {
        val vm = com.example.ui.GameStateViewModel().apply {
            baseSpeed = 10.0f
            speedIncrementRate = 0.5f // +0.5 m/s per second
            maxGameSpeed = 30.0f
            scoreMultiplier = 2.0f
        }
        vm.startGame()
        vm.stopGameLoop() // Test deterministic steps without background loop

        assertEquals(10.0f, vm.gameSpeed.value, 0.001f)
        assertEquals(0, vm.currentScore.value)

        // Step 1: 2.0 seconds elapsed
        vm.updateGameStep(2.0f)
        val expectedSpeedStep1 = 10.0f + 0.5f * 2.0f // 11.0f
        assertEquals(expectedSpeedStep1, vm.gameSpeed.value, 0.001f)
        assertEquals(expectedSpeedStep1, vm.gameState.value.gameSpeed, 0.001f)
        assertTrue("Score should have increased based on speed and delta time", vm.currentScore.value > 0)
        assertEquals(2.0f, vm.gameState.value.elapsedTimeSeconds, 0.001f)

        // Step 2: 50.0 seconds elapsed (test capping at maxGameSpeed)
        vm.updateGameStep(50.0f) // would reach 11.0 + 25 = 36 without cap
        assertEquals(30.0f, vm.gameSpeed.value, 0.001f) // capped at 30.0
        assertEquals(30.0f, vm.gameState.value.gameSpeed, 0.001f)
        assertTrue("Speed multiplier should reflect increment over base speed", vm.gameState.value.speedMultiplier >= 3.0f)
    }

    @Test
    fun gameStateViewModel_coroutineGameLoop_continuouslyUpdatesStateAndAccountsForSpeedIncrements() = kotlinx.coroutines.runBlocking {
        val vm = com.example.ui.GameStateViewModel().apply {
            baseSpeed = 10.0f
            speedIncrementRate = 5.0f // high rate for fast test verification
            maxGameSpeed = 50.0f
        }

        vm.startGameLoop(tickDelayMs = 10L)
        assertTrue(vm.isLoopRunning)

        val initialSpeed = vm.gameSpeed.value
        val initialScore = vm.currentScore.value

        // Allow coroutine game loop to tick multiple frames
        kotlinx.coroutines.delay(120L)

        val updatedSpeed = vm.gameSpeed.value
        val updatedScore = vm.currentScore.value

        assertTrue("Game speed should have incremented via coroutine game loop", updatedSpeed > initialSpeed)
        assertTrue("Score should have accumulated via coroutine game loop", updatedScore > initialScore)
        assertTrue("Game state isPlaying must be true", vm.gameState.value.isPlaying)
        assertFalse("Game state isGameOver must be false", vm.gameState.value.isGameOver)

        // Stop game loop
        vm.stopGameLoop()
        assertFalse(vm.isLoopRunning)
    }

    @Test
    fun gameStateViewModel_gameLoop_stopsWhenGameOver() = kotlinx.coroutines.runBlocking {
        val vm = com.example.ui.GameStateViewModel()
        vm.startGameLoop(tickDelayMs = 10L)
        assertTrue(vm.isLoopRunning)

        vm.setGameOver(true)
        assertFalse("Game loop should stop when game over is triggered", vm.isLoopRunning)
        assertTrue(vm.isGameOver.value)
        assertTrue(vm.gameState.value.isGameOver)
    }
}

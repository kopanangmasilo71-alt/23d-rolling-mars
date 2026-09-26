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
        val physics = GamePhysicsEngine(audio) { _, _, _ -> }
        physics.startNewGame()

        assertTrue(physics.isRunning)
        assertFalse(physics.isGameOver)
        assertEquals(32, physics.ballPool.size)
    }

    @Test
    fun touchController_inputsDrivePhysicsState() {
        val audio = GameAudio().apply { isEnabled = false }
        val physics = GamePhysicsEngine(audio) { _, _, _ -> }
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
}

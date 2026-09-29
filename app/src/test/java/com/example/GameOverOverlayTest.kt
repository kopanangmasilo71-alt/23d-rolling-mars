package com.example

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.engine.BallType
import com.example.engine.GameAudio
import com.example.engine.GamePhysicsEngine
import com.example.ui.AppScreen
import com.example.ui.GameOverOverlay
import com.example.ui.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GameOverOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun boulderCollision_triggersGameOverAndCalculatesFinalScore() {
        val audio = GameAudio().apply { isEnabled = false }
        var gameOverCalled = false
        var recordedScore = -1

        val physics = GamePhysicsEngine(audio) { score, _, _, _, _ ->
            gameOverCalled = true
            recordedScore = score
        }

        physics.startNewGame()
        physics.score = 850
        assertFalse(physics.isGameOver)

        // Spawn a boulder directly on top of the player
        physics.spawnBall(
            type = BallType.STRAIGHT,
            x = physics.player.position.x,
            z = physics.player.position.z,
            speed = 10f,
            lateralSpeed = 0f
        )

        // Step physics to process collision
        physics.update(0.016f, false, false, false)

        assertTrue("Physics must flag game over upon boulder collision", physics.isGameOver)

        // Advance frames through tumble crash timer
        for (i in 0 until 40) {
            physics.update(0.02f, false, false, false)
        }

        assertTrue("onGameOver callback should be fired after collision", gameOverCalled)
        assertTrue("Recorded score must be >= 850", recordedScore >= 850)
    }

    @Test
    fun gameOverOverlay_displaysTitleFinalScoreAndRestartButton() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GameViewModel(application)

        // Put game into game over state with a score of 1450
        viewModel.updateScore(1450)
        viewModel.setGameOver(true)

        composeTestRule.setContent {
            GameOverOverlay(viewModel = viewModel)
        }

        // 1. Verify 'Game Over' overlay is present
        composeTestRule.onNodeWithTag("game_over_overlay").assertIsDisplayed()

        // 2. Verify 'GAME OVER' title text is displayed
        composeTestRule.onNodeWithTag("game_over_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("game_over_title").assertTextContains("GAME OVER")

        // 3. Verify final score is displayed
        composeTestRule.onNodeWithTag("final_score").assertIsDisplayed()
        composeTestRule.onNodeWithTag("final_score").assertTextContains("1450")

        // 4. Verify restart button is displayed and can be clicked
        composeTestRule.onNodeWithTag("restart_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("restart_button").assertTextContains("RESTART")

        // 5. Click restart button and verify game resets
        composeTestRule.onNodeWithTag("restart_button").performClick()
        composeTestRule.waitForIdle()

        assertFalse("Game over state should be reset after restart", viewModel.isGameOver.value)
        assertEquals(AppScreen.PLAYING, viewModel.currentScreen.value)
    }

    @Test
    fun restartButton_resetsGameStateToPlaying() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GameViewModel(application)

        viewModel.setGameOver(true)
        assertTrue(viewModel.isGameOver.value)
        assertEquals(AppScreen.GAME_OVER, viewModel.currentScreen.value)

        // Call restartGame
        viewModel.restartGame()

        assertFalse(viewModel.isGameOver.value)
        assertEquals(AppScreen.PLAYING, viewModel.currentScreen.value)
        assertEquals(0, viewModel.currentScore.value)
    }
}

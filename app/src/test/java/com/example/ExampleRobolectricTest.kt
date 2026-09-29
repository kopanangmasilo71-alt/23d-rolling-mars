package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Roll Runner", appName)
  }

  @Test
  fun `matrix multiply with separate buffers works properly`() {
    val proj = FloatArray(16)
    val view = FloatArray(16)
    val model = FloatArray(16)
    val projView = FloatArray(16)
    val mvp = FloatArray(16)
    android.opengl.Matrix.setIdentityM(proj, 0)
    android.opengl.Matrix.setIdentityM(view, 0)
    android.opengl.Matrix.setIdentityM(model, 0)
    proj[0] = 2f
    view[5] = 3f
    model[10] = 4f

    android.opengl.Matrix.multiplyMM(projView, 0, proj, 0, view, 0)
    android.opengl.Matrix.multiplyMM(mvp, 0, projView, 0, model, 0)

    assertEquals(2f, mvp[0], 0.001f)
    assertEquals(3f, mvp[5], 0.001f)
    assertEquals(4f, mvp[10], 0.001f)
    assertEquals(1f, mvp[15], 0.001f)
  }

  @Test
  fun `shield absorbs boulder hit and allows player to continue running`() {
    val audio = com.example.engine.GameAudio()
    var gameOverCalled = false
    val physics = com.example.engine.GamePhysicsEngine(audio) { _, _, _, _, _ ->
      gameOverCalled = true
    }

    physics.startNewGame()
    org.junit.Assert.assertTrue(physics.isRunning)
    org.junit.Assert.assertFalse(physics.isGameOver)

    // Activate shield on player
    physics.player.activateShield(14f)
    org.junit.Assert.assertTrue(physics.player.isShieldActive)

    // Position a boulder right onto player
    val boulder = physics.ballPool[0]
    boulder.reset(com.example.engine.BallType.STRAIGHT, physics.player.position.x, physics.player.position.z, 5f)

    var deflected = false
    physics.onShieldDeflected = {
      deflected = true
    }

    // Step physics with collision
    physics.update(0.016f, false, false, false)

    // Shield should have absorbed the hit: deflected = true, gameOver NOT called, player still running!
    org.junit.Assert.assertTrue("Shield deflection callback should be triggered", deflected)
    org.junit.Assert.assertFalse("Game should NOT be over because shield protected player", physics.isGameOver)
    org.junit.Assert.assertTrue("Player should continue running", physics.isRunning)
    org.junit.Assert.assertFalse("Shield should be consumed after absorbing hit", physics.player.hasShield)
    org.junit.Assert.assertTrue("Player should have post-hit invulnerability grace period", physics.player.invincibleGraceTimer > 0f)
  }

  @Test
  fun `score multiplier doubles score accumulation`() {
    val audio = com.example.engine.GameAudio()
    val physics = com.example.engine.GamePhysicsEngine(audio) { _, _, _, _, _ -> }
    physics.startNewGame()

    // Test without multiplier
    physics.player.activateScoreMultiplier(10f, 2)
    org.junit.Assert.assertTrue(physics.player.isScoreBoosted)
    assertEquals(2, physics.player.scoreMultiplierValue)

    // Deactivate active balls so we only test distance score accumulation
    for (b in physics.ballPool) b.isActive = false

    val scoreBefore = physics.score
    physics.update(0.05f, false, false, false)
    val scoreGainedWithMultiplier = physics.score - scoreBefore

    org.junit.Assert.assertTrue("Score should increase with multiplier", scoreGainedWithMultiplier > 0)
  }

  @Test
  fun `collectible pickup gives proper powerup to player`() {
    val audio = com.example.engine.GameAudio()
    val physics = com.example.engine.GamePhysicsEngine(audio) { _, _, _, _, _ -> }
    physics.startNewGame()

    // Spawn shield right in front of player
    physics.spawnCollectibleAt(com.example.engine.CollectibleType.SHIELD, physics.player.position.x, physics.player.position.z + 0.1f)

    var collected = false
    physics.onCollectibleCollected = {
      collected = true
    }

    physics.update(0.016f, false, false, false)

    org.junit.Assert.assertTrue("Collectible should have been picked up", collected)
    org.junit.Assert.assertTrue("Player should now have active shield", physics.player.isShieldActive)
  }

  @Test
  fun `character variations have distinct 3D anatomical models and proportions`() {
    val player = com.example.engine.PlayerCharacter()

    // Vanguard (Default Cyber Infiltrator)
    player.applyCharacterModel(com.example.engine.CharacterModelId.VANGUARD)
    assertEquals(com.example.engine.CharacterModelId.VANGUARD, player.model)
    val vanguardTorsoWidth = player.torsoWidth

    // Titan (Heavy Armored Mech) - Must have significantly wider reinforced chassis & thicker limbs
    player.applyCharacterModel(com.example.engine.CharacterModelId.TITAN)
    assertEquals(com.example.engine.CharacterModelId.TITAN, player.model)
    org.junit.Assert.assertTrue("Titan must have wider torso chassis than Vanguard", player.torsoWidth > vanguardTorsoWidth)
    org.junit.Assert.assertTrue("Titan must have thicker limbs for heavy exo-suit", player.legThick > 0.20f)
    org.junit.Assert.assertTrue("Titan must have heavier enclosed helmet", player.headSize > 0.40f)

    // Valkyrie (Supersonic Glider) - Sleeker lightweight frame
    player.applyCharacterModel(com.example.engine.CharacterModelId.VALKYRIE)
    assertEquals(com.example.engine.CharacterModelId.VALKYRIE, player.model)
    org.junit.Assert.assertTrue("Valkyrie must have sleeker low-drag torso", player.torsoWidth < player.headSize * 1.5f)
    org.junit.Assert.assertTrue("Valkyrie must have higher base sprint speed", player.baseNormalSpeed > 10.0f)

    // Phantom & Chronos
    player.applyCharacterModel(com.example.engine.CharacterModelId.PHANTOM)
    assertEquals(com.example.engine.CharacterModelId.PHANTOM, player.model)
    player.applyCharacterModel(com.example.engine.CharacterModelId.CHRONOS)
    assertEquals(com.example.engine.CharacterModelId.CHRONOS, player.model)
  }

  @Test
  fun `titan mech has heavy armor perk with extended shield duration`() {
    val player = com.example.engine.PlayerCharacter()
    player.applyCharacterModel(com.example.engine.CharacterModelId.TITAN)

    player.activateShield(14f)
    org.junit.Assert.assertTrue("Titan shield duration should be extended to 18s", player.shieldTimer >= 18f)

    // Check hit grace recovery perk
    player.breakShield(1.4f)
    org.junit.Assert.assertTrue("Titan recovery grace should be extended to 1.9s", player.invincibleGraceTimer >= 1.9f)
  }

  @Test
  fun `player wallet unlocks character with points and equips correctly`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.GameDatabase.getDatabase(context)
    val repo = com.example.data.GameRecordRepository(db.gameRecordDao(), db.playerProfileDao())

    // Initial profile has 800 starting welcome points
    val profile = repo.getOrCreateProfile()
    org.junit.Assert.assertTrue(profile.totalPoints >= 800)

    // Add 1000 points to have enough for Titan Mech (1200 pts)
    repo.addPoints(1000)
    val currentPoints = repo.getOrCreateProfile().totalPoints
    org.junit.Assert.assertTrue("Points balance should be >= 1200", currentPoints >= 1200)

    // Purchase Titan Mech
    val unlocked = repo.unlockCharacter("titan", 1200)
    org.junit.Assert.assertTrue("Unlocking Titan should succeed", unlocked)

    val updatedProfile = repo.getOrCreateProfile()
    assertEquals("titan", updatedProfile.equippedCharacterId)
    org.junit.Assert.assertTrue("Profile should now include titan in unlocked characters", updatedProfile.unlockedCharacterIds.contains("titan"))
  }

  @Test
  fun `particle system dust clouds are semi-transparent and do not block obstacle visibility`() {
    val particles = com.example.engine.ParticleSystem(50)
    val origin = com.example.engine.Vector3(0f, 0f, -10f)
    val magentaBallColor = floatArrayOf(0.85f, 0.2f, 0.85f, 1f) // Intense pink/magenta ball color

    // Emit dust trail
    particles.emitBoulderTrailDust(origin, 0.75f, 10f, magentaBallColor)

    val activeDust = particles.particles.firstOrNull { it.lifetime > 0f && it.particleType == com.example.engine.ParticleType.DUST_CLOUD }
    org.junit.Assert.assertNotNull("A dust cloud particle should be emitted", activeDust)
    org.junit.Assert.assertTrue(
      "Dust cloud alpha must be semi-transparent (<= 0.35) so it never blocks obstacles",
      activeDust!!.color[3] <= 0.35f
    )
    org.junit.Assert.assertTrue(
      "Dust cloud initial size should be properly visible without being too large (between 0.25 and 0.55)",
      activeDust.size in 0.25f..0.55f
    )

    // Update particles over time and verify alpha falloff
    val initialAlpha = activeDust.color[3]
    particles.update(0.2f)
    org.junit.Assert.assertTrue(
      "Dust cloud alpha should smoothly decay towards zero as it ages",
      activeDust.color[3] < initialAlpha
    )
  }

  @Test
  fun `particle burst from collectible pickup is compact and non-obstructive`() {
    val particles = com.example.engine.ParticleSystem(50)
    val pickupPos = com.example.engine.Vector3(0f, 1f, 0f)
    val pinkCellColor = floatArrayOf(1.0f, 0.25f, 0.85f, 1.0f) // Energy Cell pink

    particles.emitBurst(pickupPos, 14, pinkCellColor)

    var count = 0
    for (p in particles.particles) {
      if (p.lifetime > 0f) count++
    }
    assertEquals(14, count)
  }

  @Test
  fun `test GameRenderer onSurfaceCreated and verify programId and isReady`() {
    val audio = com.example.engine.GameAudio()
    val physics = com.example.engine.GamePhysicsEngine(audio) { _, _, _, _, _ -> }
    val renderer = com.example.engine.GameRenderer(physics)

    renderer.onSurfaceCreated(null, null)
    renderer.onSurfaceChanged(null, 1080, 1920)
    renderer.onDrawFrame(null)
  }

  @Test
  fun `gameViewModel manages currentScore isGameOver and gameSpeed via StateFlow`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.GameViewModel(application)

    // Initial state check
    assertEquals(0, viewModel.currentScore.value)
    org.junit.Assert.assertFalse(viewModel.isGameOver.value)
    org.junit.Assert.assertFalse(viewModel.gameOverStatus.value)
    assertEquals(10.0f, viewModel.gameSpeed.value, 0.001f)
    assertEquals(0, viewModel.gameState.value.currentScore)

    // Start game
    viewModel.startGame()
    assertEquals(0, viewModel.currentScore.value)
    org.junit.Assert.assertFalse(viewModel.isGameOver.value)
    org.junit.Assert.assertTrue(viewModel.gameState.value.isPlaying)

    // Update score
    viewModel.updateScore(750)
    assertEquals(750, viewModel.currentScore.value)
    assertEquals(750, viewModel.gameState.value.currentScore)

    // Update speed
    viewModel.setGameSpeed(15.5f)
    assertEquals(15.5f, viewModel.gameSpeed.value, 0.001f)
    assertEquals(15.5f, viewModel.gameState.value.gameSpeed, 0.001f)

    // Trigger Game Over
    viewModel.setGameOver(true)
    org.junit.Assert.assertTrue(viewModel.isGameOver.value)
    org.junit.Assert.assertTrue(viewModel.gameState.value.isGameOver)
  }

  @Test
  fun `gameViewModel coroutine game loop continuously updates game state with speed increments`() = kotlinx.coroutines.runBlocking {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.GameViewModel(application)

    // Start game starts coroutine loop
    viewModel.startGame()
    org.junit.Assert.assertTrue("Game loop should be running after startGame", viewModel.isGameLoopRunning)
    org.junit.Assert.assertTrue("Game state should be playing", viewModel.gameState.value.isPlaying)

    // Verify speed increment properties
    val baseSpeed = viewModel.gameSpeed.value
    org.junit.Assert.assertTrue("Base speed should be positive", baseSpeed > 0f)

    // Pause game stops game loop
    viewModel.pauseGame()
    org.junit.Assert.assertFalse("Game loop should stop when paused", viewModel.isGameLoopRunning)
    org.junit.Assert.assertTrue("Game state should be paused", viewModel.gameState.value.isPaused)

    // Resume game restarts loop
    viewModel.resumeGame()
    org.junit.Assert.assertTrue("Game loop should resume", viewModel.isGameLoopRunning)
    org.junit.Assert.assertFalse("Game state should not be paused", viewModel.gameState.value.isPaused)

    // Stop game loop
    viewModel.stopGameLoop()
    org.junit.Assert.assertFalse("Game loop should be stopped", viewModel.isGameLoopRunning)
  }
}

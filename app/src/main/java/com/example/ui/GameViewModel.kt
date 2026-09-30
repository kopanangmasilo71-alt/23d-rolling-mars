package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CareerStats
import com.example.data.GameDatabase
import com.example.data.GameRecord
import com.example.data.GameRecordRepository
import com.example.engine.GameAudio
import com.example.engine.GamePhysicsEngine
import com.example.engine.GameRenderer
import com.example.engine.SectorInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppScreen {
    MENU,
    PLAYING,
    PAUSED,
    GAME_OVER
}

enum class ImpactType {
    FATAL_BOULDER_CRASH,
    SHIELD_DEFLECT,
    OVERDRIVE_CRUSH,
    BOULDER_BLAST
}

data class CollisionImpactEvent(
    val id: Long = 0L,
    val normalizedX: Float = 0.5f,
    val normalizedY: Float = 0.68f,
    val screenX: Float = 0f,
    val screenY: Float = 0f,
    val impactType: ImpactType = ImpactType.FATAL_BOULDER_CRASH
)

data class LiveGameStats(
    val score: Int = 0,
    val distanceMeters: Int = 0,
    val ballsDodged: Int = 0,
    val comboMultiplier: Int = 1,
    val comboProgress: Float = 0f,
    val forwardSpeed: Float = 10f,
    val isBraking: Boolean = false,
    val isBoosting: Boolean = false,
    val boostProgress: Float = 0f,
    val boostRemainingSec: Int = 0,
    val isGrounded: Boolean = true,
    val sectorName: String = "SECTOR 1",
    val sectorSubtitle: String = "OUTPOST DAWN",
    val isOverdrive: Boolean = false,
    val highScoreToBeat: Int = 0,
    val hasBeatenHighScore: Boolean = false,
    val hasShield: Boolean = false,
    val shieldProgress: Float = 0f,
    val shieldRemainingSec: Int = 0,
    val isScoreBoosted: Boolean = false,
    val scoreMultiplierProgress: Float = 0f,
    val scoreMultiplierRemainingSec: Int = 0,
    val scoreMultiplierValue: Int = 1,
    val timeOfDayName: String = "DAWN",
    val timeOfDayEmoji: String = "🌅",
    val timeOfDayTime: String = "06:00 AM",
    val timeOfDayDayNumber: Int = 1,
    val timeOfDayCycleProgress: Float = 0f,
    val orbsCollected: Int = 0,
    val maxOrbsForBoost: Int = 3,
    val ammo: Int = 10,
    val maxAmmo: Int = 30,
    val runDurationSeconds: Int = 0,
    val dynamicSpeedMultiplier: Float = 1.0f,
    val dynamicFrequencyMultiplier: Float = 1.0f,
    val threatLevel: Int = 1,
    val threatLevelName: String = "STABLE",
    val threatLevelColorHex: Long = 0xFF00E5FF,
    val waveIndex: Int = 1,
    val wavePatternName: String = "SOLO PATROL",
    val currentSpawnInterval: Float = 2.2f,
    val isBreatherWave: Boolean = false,
    val styleRank: String = "D",
    val styleMultiplier: Float = 1.0f,
    val styleScore: Float = 0f,
    val overdriveEnergy: Float = 0.25f,
    val isOverdriveActive: Boolean = false,
    val isDashing: Boolean = false
)

data class GameOverSummary(
    val score: Int = 0,
    val distanceMeters: Int = 0,
    val ballsDodged: Int = 0,
    val maxCombo: Int = 1,
    val sectorReached: String = "Sector 1",
    val difficultyMode: String = "Standard",
    val isNewHighScore: Boolean = false,
    val performanceGrade: String = "B",
    val xpEarned: Int = 0,
    val pointsEarned: Int = 0,
    val totalWalletPoints: Int = 0,
    val runDurationSeconds: Int = 0,
    val maxThreatLevelReached: Int = 1
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRecordRepository
    val soundManager = com.example.audio.SoundManager.getInstance(application)
    val audio = GameAudio().apply {
        soundManager = this@GameViewModel.soundManager
    }

    private val _currentScreen = MutableStateFlow(AppScreen.MENU)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _currentScore = MutableStateFlow(0)
    val currentScore: StateFlow<Int> = _currentScore.asStateFlow()

    private val _isGameOver = MutableStateFlow(false)
    val isGameOver: StateFlow<Boolean> = _isGameOver.asStateFlow()
    val gameOverStatus: StateFlow<Boolean> = _isGameOver

    private val _gameSpeed = MutableStateFlow(10.0f)
    val gameSpeed: StateFlow<Float> = _gameSpeed.asStateFlow()

    fun updateScore(score: Int) {
        _currentScore.value = score
        _gameState.update { it.copy(currentScore = score) }
    }

    fun addScore(points: Int) {
        val newScore = _currentScore.value + points
        updateScore(newScore)
    }

    fun setGameOver(isOver: Boolean) {
        _isGameOver.value = isOver
        if (isOver) {
            _currentScreen.value = AppScreen.GAME_OVER
        }
        _gameState.update {
            it.copy(
                isGameOver = isOver,
                isPlaying = if (isOver) false else it.isPlaying
            )
        }
    }

    fun setGameSpeed(speed: Float) {
        _gameSpeed.value = speed
        _gameState.update { it.copy(gameSpeed = speed) }
    }

    private var gameLoopJob: Job? = null
    val isGameLoopRunning: Boolean
        get() = gameLoopJob?.isActive == true

    /**
     * Starts a frame-independent game loop using a Coroutine that continuously
     * updates the game state in the ViewModel, accounting for speed increments.
     */
    fun startGameLoop(tickDelayMs: Long = 16L) {
        stopGameLoop()
        gameLoopJob = viewModelScope.launch {
            var lastTimeNanos = System.nanoTime()
            while (isActive && _currentScreen.value == AppScreen.PLAYING && !_isGameOver.value) {
                val now = System.nanoTime()
                val dt = if (lastTimeNanos != 0L) {
                    ((now - lastTimeNanos) / 1_000_000_000.0f).coerceIn(0.001f, 0.1f)
                } else {
                    0.016f
                }
                lastTimeNanos = now

                pollStats(dt)

                delay(tickDelayMs)
            }
        }
    }

    fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    fun resetGame() {
        startGame()
    }

    private val _liveStats = MutableStateFlow(LiveGameStats())
    val liveStats: StateFlow<LiveGameStats> = _liveStats.asStateFlow()

    private val _gameOverSummary = MutableStateFlow(GameOverSummary())
    val gameOverSummary: StateFlow<GameOverSummary> = _gameOverSummary.asStateFlow()

    private val _isNewHighScore = MutableStateFlow(false)
    val isNewHighScore: StateFlow<Boolean> = _isNewHighScore.asStateFlow()

    private val _characterColor = MutableStateFlow("Vanguard Striker")
    val characterColor: StateFlow<String> = _characterColor.asStateFlow()

    private val _isOverdriveMode = MutableStateFlow(false)
    val isOverdriveMode: StateFlow<Boolean> = _isOverdriveMode.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _bgmEnabled = MutableStateFlow(true)
    val bgmEnabled: StateFlow<Boolean> = _bgmEnabled.asStateFlow()

    fun toggleBgm() {
        val nv = !_bgmEnabled.value
        _bgmEnabled.value = nv
        audio.isBgmEnabled = nv
        soundManager.playMenuClick()
        if (nv && _currentScreen.value == AppScreen.PLAYING) {
            audio.startBgm()
        } else {
            audio.stopBgm()
        }
    }

    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _sectorAnnouncement = MutableStateFlow<SectorInfo?>(null)
    val sectorAnnouncement: StateFlow<SectorInfo?> = _sectorAnnouncement.asStateFlow()

    private val _nearMissFlash = MutableStateFlow(false)
    val nearMissFlash: StateFlow<Boolean> = _nearMissFlash.asStateFlow()

    private val _shieldDeflectedAlert = MutableStateFlow(false)
    val shieldDeflectedAlert: StateFlow<Boolean> = _shieldDeflectedAlert.asStateFlow()

    private val _collectiblePickupAlert = MutableStateFlow<com.example.engine.CollectibleType?>(null)
    val collectiblePickupAlert: StateFlow<com.example.engine.CollectibleType?> = _collectiblePickupAlert.asStateFlow()

    private val _dodgeFeedbackAlert = MutableStateFlow<String?>(null)
    val dodgeFeedbackAlert: StateFlow<String?> = _dodgeFeedbackAlert.asStateFlow()

    private val _scorePopupDelta = MutableStateFlow<String?>(null)
    val scorePopupDelta: StateFlow<String?> = _scorePopupDelta.asStateFlow()

    private val _timeOfDayAnnouncement = MutableStateFlow<String?>(null)
    val timeOfDayAnnouncement: StateFlow<String?> = _timeOfDayAnnouncement.asStateFlow()
    private var lastAnnouncedPhase: com.example.engine.TimeOfDayPhase? = null

    private val _threatEscalationAnnouncement = MutableStateFlow<com.example.engine.DynamicThreatLevel?>(null)
    val threatEscalationAnnouncement: StateFlow<com.example.engine.DynamicThreatLevel?> = _threatEscalationAnnouncement.asStateFlow()

    private val _screenShakeTrigger = MutableStateFlow(0L)
    val screenShakeTrigger: StateFlow<Long> = _screenShakeTrigger.asStateFlow()

    private val _screenShakeIntensity = MutableStateFlow(0f)
    val screenShakeIntensity: StateFlow<Float> = _screenShakeIntensity.asStateFlow()

    private val _collisionImpactEvent = MutableStateFlow<CollisionImpactEvent?>(null)
    val collisionImpactEvent: StateFlow<CollisionImpactEvent?> = _collisionImpactEvent.asStateFlow()

    fun triggerCollisionImpact(
        normalizedX: Float = 0.5f,
        normalizedY: Float = 0.68f,
        type: ImpactType = ImpactType.FATAL_BOULDER_CRASH
    ) {
        _collisionImpactEvent.value = CollisionImpactEvent(
            id = System.nanoTime(),
            normalizedX = normalizedX,
            normalizedY = normalizedY,
            impactType = type
        )
    }

    fun triggerScreenShake(intensity: Float) {
        _screenShakeIntensity.value = intensity
        _screenShakeTrigger.value = System.currentTimeMillis()
    }

    val topRecord: StateFlow<GameRecord?>
    val top10Records: StateFlow<List<GameRecord>>
    val allRecords: StateFlow<List<GameRecord>>
    val careerStats: StateFlow<CareerStats>
    val playerProfile: StateFlow<com.example.data.PlayerProfile?>

    val physics: GamePhysicsEngine
    val renderer: GameRenderer

    init {
        val db = GameDatabase.getDatabase(application)
        repository = GameRecordRepository(db.gameRecordDao(), db.playerProfileDao())

        playerProfile = repository.playerProfile.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            com.example.data.PlayerProfile()
        )

        topRecord = repository.topRecord.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

        top10Records = repository.top10Records.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allRecords = repository.allRecords.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        careerStats = repository.careerStats.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            CareerStats()
        )

        physics = GamePhysicsEngine(audio) { score, distance, dodged, maxCombo, sectorName ->
            onGameEnded(score, distance, dodged, maxCombo, sectorName)
        }

        viewModelScope.launch {
            val prof = repository.getOrCreateProfile()
            val m = com.example.engine.CharacterModelId.fromId(prof.equippedCharacterId)
            physics.player.applyCharacterModel(m)
            _characterColor.value = m.displayName
        }

        physics.onSectorChanged = { sector ->
            _sectorAnnouncement.value = sector
            triggerHaptic(120)
            viewModelScope.launch {
                delay(3000)
                if (_sectorAnnouncement.value == sector) {
                    _sectorAnnouncement.value = null
                }
            }
        }

        physics.onNearMissEvent = {
            _nearMissFlash.value = true
            triggerHaptic(40)
            viewModelScope.launch {
                delay(800)
                _nearMissFlash.value = false
            }
        }

        physics.onShieldDeflected = {
            _shieldDeflectedAlert.value = true
            viewModelScope.launch {
                delay(2000)
                if (_shieldDeflectedAlert.value == true) {
                    _shieldDeflectedAlert.value = false
                }
            }
        }

        physics.onPlayerHitObstacle = { _, isShieldBreak ->
            triggerObstacleCollisionHaptic(isFatal = !isShieldBreak)
            val normX = ((physics.player.position.x / (physics.roadHalfWidth * 1.15f)) * 0.45f + 0.5f).coerceIn(0.12f, 0.88f)
            val normY = 0.68f
            val impactType = if (isShieldBreak) ImpactType.SHIELD_DEFLECT else ImpactType.FATAL_BOULDER_CRASH
            triggerCollisionImpact(normX, normY, impactType)

            if (!isShieldBreak) {
                triggerScreenShake(32.0f)
            } else {
                triggerScreenShake(18.0f)
            }
        }

        physics.onCollectibleCollected = { type ->
            _collectiblePickupAlert.value = type
            triggerHaptic(50)
            viewModelScope.launch {
                delay(1600)
                if (_collectiblePickupAlert.value == type) {
                    _collectiblePickupAlert.value = null
                }
            }
        }

        physics.onDodgeFeedback = { text, gain, _ ->
            _dodgeFeedbackAlert.value = text
            _scorePopupDelta.value = "+$gain"
            triggerHaptic(35)
            viewModelScope.launch {
                delay(1200)
                if (_dodgeFeedbackAlert.value == text) {
                    _dodgeFeedbackAlert.value = null
                }
                _scorePopupDelta.value = null
            }
        }

        physics.onShootFired = {
            triggerHaptic(50, heavy = false)
            triggerScreenShake(9.0f)
        }

        physics.onBoulderDestroyed = {
            triggerHaptic(200, heavy = true)
            triggerScreenShake(24.0f)
            val normX = ((physics.player.position.x / (physics.roadHalfWidth * 1.15f)) * 0.45f + 0.5f).coerceIn(0.12f, 0.88f)
            val impactType = if (physics.player.isOverdriveActive) ImpactType.OVERDRIVE_CRUSH else ImpactType.BOULDER_BLAST
            val normY = if (physics.player.isOverdriveActive) 0.68f else 0.48f
            triggerCollisionImpact(normX, normY, impactType)
        }

        physics.onOutOfAmmo = {
            triggerHaptic(25, heavy = false)
            triggerScreenShake(3.5f)
        }

        physics.onThreatEscalation = { threat ->
            _threatEscalationAnnouncement.value = threat
            triggerHaptic(80, heavy = true)
            triggerScreenShake(14.0f)
            viewModelScope.launch {
                delay(3200)
                if (_threatEscalationAnnouncement.value?.level == threat.level) {
                    _threatEscalationAnnouncement.value = null
                }
            }
        }

        physics.onHazardWaveSpawned = { waveIndex, pattern, interval ->
            if (pattern != com.example.engine.HazardPattern.SOLO_PATROL) {
                val intervalStr = String.format(java.util.Locale.US, "%.1fs", interval)
                val alertMsg = "WAVE $waveIndex • ${pattern.displayName.uppercase()} ($intervalStr)"
                _dodgeFeedbackAlert.value = alertMsg
                viewModelScope.launch {
                    delay(1400)
                    if (_dodgeFeedbackAlert.value == alertMsg) {
                        _dodgeFeedbackAlert.value = null
                    }
                }
            }
        }

        renderer = GameRenderer(physics)
    }

    fun setOverdriveMode(enabled: Boolean) {
        if (_isOverdriveMode.value != enabled) {
            _isOverdriveMode.value = enabled
            soundManager.playMenuClick()
        }
    }

    fun startGame() {
        soundManager.playMenuSelect()
        _isNewHighScore.value = false
        _isGameOver.value = false
        _currentScore.value = 0
        _gameSpeed.value = 10.0f
        _gameState.value = GameState(
            currentScore = 0,
            isGameOver = false,
            gameSpeed = 10.0f,
            isPlaying = true,
            isPaused = false
        )
        _sectorAnnouncement.value = null
        _threatEscalationAnnouncement.value = null
        _nearMissFlash.value = false
        _shieldDeflectedAlert.value = false
        _collectiblePickupAlert.value = null
        _timeOfDayAnnouncement.value = null
        lastAnnouncedPhase = com.example.engine.TimeOfDayPhase.DAWN
        val equippedId = playerProfile.value?.equippedCharacterId ?: "vanguard"
        val model = com.example.engine.CharacterModelId.fromId(equippedId)
        physics.startNewGame(model.id, _isOverdriveMode.value)
        _currentScreen.value = AppScreen.PLAYING
        if (_bgmEnabled.value && _soundEnabled.value) {
            audio.startBgm()
        }
        startGameLoop()
    }

    fun pauseGame() {
        if (_currentScreen.value == AppScreen.PLAYING) {
            soundManager.playMenuClick()
            audio.stopBgm()
            physics.isRunning = false
            _currentScreen.value = AppScreen.PAUSED
            _gameState.update { it.copy(isPaused = true) }
            stopGameLoop()
        }
    }

    fun resumeGame() {
        if (_currentScreen.value == AppScreen.PAUSED) {
            soundManager.playMenuSelect()
            physics.isRunning = true
            _currentScreen.value = AppScreen.PLAYING
            _gameState.update { it.copy(isPaused = false) }
            if (_bgmEnabled.value && _soundEnabled.value) {
                audio.startBgm()
            }
            startGameLoop()
        }
    }

    fun goToMenu() {
        soundManager.playMenuBack()
        audio.stopBgm()
        stopGameLoop()
        physics.isRunning = false
        physics.clearAllTrackEntities()
        _currentScreen.value = AppScreen.MENU
        _gameState.update { it.copy(isPlaying = false, isPaused = false) }
    }

    fun restartGame() {
        soundManager.playMenuSelect()
        startGame()
    }

    fun setCharacterColor(colorPreset: String) {
        _characterColor.value = colorPreset
        val model = com.example.engine.CharacterModelId.fromId(colorPreset)
        physics.player.applyCharacterModel(model)
        viewModelScope.launch {
            repository.equipCharacter(model.id)
        }
    }

    fun selectOrPurchaseCharacter(model: com.example.engine.CharacterModelId) {
        viewModelScope.launch {
            val prof = repository.getOrCreateProfile()
            val unlocked = prof.unlockedCharacterIds.split(",").map { it.trim().lowercase() }.toSet()
            if (unlocked.contains(model.id.lowercase())) {
                repository.equipCharacter(model.id)
                physics.player.applyCharacterModel(model)
                _characterColor.value = model.displayName
                audio.playCharacterEquip()
                soundManager.playMenuSelect()
                triggerHaptic(40)
            } else {
                val success = repository.unlockCharacter(model.id, model.price)
                if (success) {
                    physics.player.applyCharacterModel(model)
                    _characterColor.value = model.displayName
                    audio.playUnlockSuccess()
                    soundManager.playMenuSelect()
                    triggerHaptic(120)
                } else {
                    audio.playBrake()
                    soundManager.playMenuBack()
                    triggerHaptic(180)
                }
            }
        }
    }

    fun addBonusPoints(amount: Int = 1000) {
        viewModelScope.launch {
            repository.addPoints(amount)
            audio.playPowerUpPickup()
            triggerHaptic(50)
        }
    }

    fun toggleSound() {
        val newVal = !_soundEnabled.value
        _soundEnabled.value = newVal
        audio.isEnabled = newVal
        soundManager.isEnabled = newVal
        if (newVal) {
            soundManager.playMenuClick()
        }
    }

    fun toggleVibration() {
        _vibrationEnabled.value = !_vibrationEnabled.value
        soundManager.playMenuClick()
    }

    fun playMenuClick() {
        soundManager.playMenuClick()
    }

    fun playMenuSelect() {
        soundManager.playMenuSelect()
    }

    fun playMenuBack() {
        soundManager.playMenuBack()
    }

    fun playBoulderCollision() {
        soundManager.playBoulderCollision()
    }

    fun playJumpSound() {
        soundManager.playJump()
    }

    fun jump() {
        physics.jump()
        triggerHaptic(45)
    }

    fun triggerDash(direction: Float): Boolean {
        val ok = physics.triggerDash(direction)
        if (ok) triggerHaptic(35)
        return ok
    }

    fun activateOverdrive(): Boolean {
        val ok = physics.activateOverdrive()
        if (ok) triggerHaptic(140)
        return ok
    }

    fun setLeftHeld(held: Boolean) {
        renderer.isLeftHeld = held
    }

    fun setRightHeld(held: Boolean) {
        renderer.isRightHeld = held
    }

    fun setBrakeHeld(held: Boolean) {
        renderer.isBrakeHeld = held
    }

    fun shoot() {
        physics.shoot()
        triggerHaptic(50)
    }

    fun setShootHeld(held: Boolean) {
        renderer.isShootHeld = held
        physics.isShootHeld = held
        if (held) {
            physics.shoot()
            triggerHaptic(40)
        }
    }

    fun pollStats(dt: Float = 0.016f) {
        if (_currentScreen.value == AppScreen.PLAYING) {
            val best = topRecord.value?.score ?: 0
            val currentScore = physics.score
            val beaten = best > 0 && currentScore > best

            val tod = renderer.currentTimeOfDay
            if (lastAnnouncedPhase != null && lastAnnouncedPhase != tod.phase) {
                lastAnnouncedPhase = tod.phase
                _timeOfDayAnnouncement.value = "${tod.phase.bannerText}  •  DAY ${tod.dayNumber}"
                viewModelScope.launch {
                    delay(2600)
                    if (_timeOfDayAnnouncement.value?.contains(tod.phase.bannerText) == true) {
                        _timeOfDayAnnouncement.value = null
                    }
                }
            }

            val shieldRem = if (physics.player.isShieldActive) kotlin.math.ceil(physics.player.shieldTimer).toInt() else 0
            val multRem = if (physics.player.isScoreBoosted) kotlin.math.ceil(physics.player.scoreMultiplierTimer).toInt() else 0
            val boostRem = if (physics.player.isBoosting) kotlin.math.ceil(physics.player.boostTimer).toInt() else 0

            val calculatedSpeed = physics.player.forwardSpeed * physics.currentSector.speedMultiplier
            _currentScore.value = currentScore
            _gameSpeed.value = calculatedSpeed
            _isGameOver.value = false
            _gameState.value = GameState(
                currentScore = currentScore,
                isGameOver = false,
                gameSpeed = calculatedSpeed,
                isPaused = false,
                isPlaying = true,
                distanceMeters = physics.distanceTraveled.toInt(),
                ballsDodged = physics.ballsDodged,
                comboMultiplier = physics.comboMultiplier,
                sectorName = physics.currentSector.name,
                speedMultiplier = physics.dynamicSpeedMultiplier,
                elapsedTimeSeconds = physics.runDuration
            )

            _liveStats.value = LiveGameStats(
                score = currentScore,
                distanceMeters = physics.distanceTraveled.toInt(),
                ballsDodged = physics.ballsDodged,
                comboMultiplier = physics.comboMultiplier,
                comboProgress = if (physics.comboTimer > 0f) (physics.comboTimer / physics.maxComboTimer).coerceIn(0f, 1f) else 0f,
                forwardSpeed = calculatedSpeed,
                isBraking = renderer.isBrakeHeld,
                isBoosting = physics.player.isBoosting,
                boostProgress = if (physics.player.isBoosting) (physics.player.boostTimer / 4.0f).coerceIn(0f, 1f) else 0f,
                boostRemainingSec = boostRem,
                isGrounded = physics.player.isGrounded,
                sectorName = physics.currentSector.name,
                sectorSubtitle = physics.currentSector.subtitle,
                isOverdrive = physics.isOverdriveMode,
                highScoreToBeat = best,
                hasBeatenHighScore = beaten,
                hasShield = physics.player.isShieldActive,
                shieldProgress = if (physics.player.isShieldActive && physics.player.maxShieldDuration > 0f) {
                    (physics.player.shieldTimer / physics.player.maxShieldDuration).coerceIn(0f, 1f)
                } else 0f,
                shieldRemainingSec = shieldRem,
                isScoreBoosted = physics.player.isScoreBoosted,
                scoreMultiplierProgress = if (physics.player.isScoreBoosted && physics.player.maxMultiplierDuration > 0f) {
                    (physics.player.scoreMultiplierTimer / physics.player.maxMultiplierDuration).coerceIn(0f, 1f)
                } else 0f,
                scoreMultiplierRemainingSec = multRem,
                scoreMultiplierValue = physics.player.scoreMultiplierValue,
                timeOfDayName = tod.phase.title,
                timeOfDayEmoji = tod.phase.emoji,
                timeOfDayTime = tod.timeString,
                timeOfDayDayNumber = tod.dayNumber,
                timeOfDayCycleProgress = tod.cycleProgress,
                orbsCollected = physics.orbsCollected,
                maxOrbsForBoost = physics.maxOrbsForBoost,
                ammo = physics.player.ammo,
                maxAmmo = physics.player.maxAmmo,
                runDurationSeconds = physics.runDurationSeconds,
                dynamicSpeedMultiplier = physics.dynamicSpeedMultiplier,
                dynamicFrequencyMultiplier = physics.dynamicFrequencyMultiplier,
                threatLevel = physics.currentThreatLevel.level,
                threatLevelName = physics.currentThreatLevel.name,
                threatLevelColorHex = physics.currentThreatLevel.badgeColorHex,
                waveIndex = physics.waveCount,
                wavePatternName = physics.currentHazardPattern.displayName,
                currentSpawnInterval = physics.currentSpawnInterval,
                isBreatherWave = physics.isBreatherWave,
                styleRank = physics.player.styleRank,
                styleMultiplier = physics.player.styleMultiplier,
                styleScore = physics.player.styleScore,
                overdriveEnergy = physics.player.overdriveEnergy,
                isOverdriveActive = physics.player.isOverdriveActive,
                isDashing = physics.player.isDashing
            )
        }
    }

    private fun onGameEnded(
        score: Int,
        distance: Int,
        dodged: Int,
        maxCombo: Int,
        sectorName: String
    ) {
        triggerHaptic(280)

        val previousBest = topRecord.value?.score ?: 0
        val isNewBest = score > previousBest && score > 0
        _isNewHighScore.value = isNewBest
        if (isNewBest) {
            audio.playHighScoreChime()
        }

        // Grade calculation
        val grade = when {
            score >= 4200 || maxCombo >= 5 -> "S"
            score >= 2200 || maxCombo >= 4 -> "A"
            score >= 1100 -> "B"
            else -> "C"
        }
        val xpEarned = score / 5

        val basePoints = score
        val dodgeBonus = dodged * 15
        val comboBonus = maxCombo * 40
        val characterBonus = if (physics.player.model == com.example.engine.CharacterModelId.CHRONOS) 1.25f else 1.0f
        val pointsEarned = ((basePoints + dodgeBonus + comboBonus) * characterBonus).toInt()

        audio.stopBgm()
        stopGameLoop()
        _isGameOver.value = true
        _currentScore.value = score
        _gameState.update {
            it.copy(
                currentScore = score,
                isGameOver = true,
                isPlaying = false,
                distanceMeters = distance,
                ballsDodged = dodged,
                comboMultiplier = maxCombo,
                sectorName = sectorName
            )
        }

        viewModelScope.launch {
            repository.addPoints(pointsEarned)
            val updatedProfile = repository.getOrCreateProfile()

            _gameOverSummary.value = GameOverSummary(
                score = score,
                distanceMeters = distance,
                ballsDodged = dodged,
                maxCombo = maxCombo,
                sectorReached = sectorName,
                difficultyMode = if (_isOverdriveMode.value) "Overdrive" else "Standard",
                isNewHighScore = isNewBest,
                performanceGrade = grade,
                xpEarned = xpEarned,
                pointsEarned = pointsEarned,
                totalWalletPoints = updatedProfile.totalPoints,
                runDurationSeconds = physics.runDurationSeconds,
                maxThreatLevelReached = physics.maxThreatLevelReached
            )

            repository.saveRecord(
                GameRecord(
                    score = score,
                    distanceMeters = distance,
                    ballsDodged = dodged,
                    characterColor = _characterColor.value,
                    difficultyMode = if (_isOverdriveMode.value) "Overdrive" else "Standard",
                    maxCombo = maxCombo,
                    sectorReached = sectorName
                )
            )
            _currentScreen.value = AppScreen.GAME_OVER
        }
    }

    /**
     * Dedicated obstacle collision and damage haptic feedback.
     * Generates a realistic tactile vibration response:
     * - Fatal collision: Heavy multi-burst shuddering rumble (strong initial jolt decaying into high-frequency impact ripples).
     * - Shield damage / deflection: Crisp high-energy double-pulse vibration.
     */
    fun triggerObstacleCollisionHaptic(isFatal: Boolean) {
        if (!_vibrationEnabled.value) return
        if (isFatal) {
            // Fatal direct boulder collision: Heavy multi-burst shuddering rumble (430ms total)
            triggerHapticPattern(
                timings = longArrayOf(0, 180, 45, 130, 30, 90),
                amplitudes = intArrayOf(0, 255, 0, 220, 0, 150)
            )
        } else {
            // Shield deflection & damage absorption: Crisp metallic shockwave double-pulse (275ms total)
            triggerHapticPattern(
                timings = longArrayOf(0, 95, 35, 115),
                amplitudes = intArrayOf(0, 220, 0, 170)
            )
        }
    }

    /**
     * Executes custom waveform vibration patterns on supported devices,
     * with graceful fallback for devices without amplitude control or older APIs.
     */
    fun triggerHapticPattern(timings: LongArray, amplitudes: IntArray? = null) {
        if (!_vibrationEnabled.value) return
        try {
            val context = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (amplitudes != null && amplitudes.size == timings.size && vibrator.hasAmplitudeControl()) {
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, -1)
            }
        } catch (_: Throwable) {
            // Ignore if vibration unavailable
        }
    }

    fun triggerHaptic(durationMs: Long, heavy: Boolean = false) {
        if (!_vibrationEnabled.value) return
        try {
            val context = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitude = if (heavy) VibrationEffect.DEFAULT_AMPLITUDE else 180
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        durationMs,
                        amplitude
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (_: Throwable) {
            // Ignore if vibration unavailable
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopGameLoop()
        soundManager.release()
    }
}

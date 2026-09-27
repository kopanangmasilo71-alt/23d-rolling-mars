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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    MENU,
    PLAYING,
    PAUSED,
    GAME_OVER
}

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
    val timeOfDayCycleProgress: Float = 0f
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
    val totalWalletPoints: Int = 0
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRecordRepository
    val audio = GameAudio()

    private val _currentScreen = MutableStateFlow(AppScreen.MENU)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

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

    private val _timeOfDayAnnouncement = MutableStateFlow<String?>(null)
    val timeOfDayAnnouncement: StateFlow<String?> = _timeOfDayAnnouncement.asStateFlow()
    private var lastAnnouncedPhase: com.example.engine.TimeOfDayPhase? = null

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
            triggerHaptic(160)
            viewModelScope.launch {
                delay(2000)
                _shieldDeflectedAlert.value = false
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

        renderer = GameRenderer(physics)
    }

    fun setOverdriveMode(enabled: Boolean) {
        _isOverdriveMode.value = enabled
    }

    fun startGame() {
        _isNewHighScore.value = false
        _sectorAnnouncement.value = null
        _nearMissFlash.value = false
        _shieldDeflectedAlert.value = false
        _collectiblePickupAlert.value = null
        _timeOfDayAnnouncement.value = null
        lastAnnouncedPhase = com.example.engine.TimeOfDayPhase.DAWN
        val equippedId = playerProfile.value?.equippedCharacterId ?: "vanguard"
        val model = com.example.engine.CharacterModelId.fromId(equippedId)
        physics.startNewGame(model.id, _isOverdriveMode.value)
        _currentScreen.value = AppScreen.PLAYING
    }

    fun pauseGame() {
        if (_currentScreen.value == AppScreen.PLAYING) {
            physics.isRunning = false
            _currentScreen.value = AppScreen.PAUSED
        }
    }

    fun resumeGame() {
        if (_currentScreen.value == AppScreen.PAUSED) {
            physics.isRunning = true
            _currentScreen.value = AppScreen.PLAYING
        }
    }

    fun goToMenu() {
        physics.isRunning = false
        physics.clearAllTrackEntities()
        _currentScreen.value = AppScreen.MENU
    }

    fun restartGame() {
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
                triggerHaptic(40)
            } else {
                val success = repository.unlockCharacter(model.id, model.price)
                if (success) {
                    physics.player.applyCharacterModel(model)
                    _characterColor.value = model.displayName
                    audio.playUnlockSuccess()
                    triggerHaptic(120)
                } else {
                    audio.playBrake()
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
    }

    fun toggleVibration() {
        _vibrationEnabled.value = !_vibrationEnabled.value
    }

    fun jump() {
        physics.jump()
        triggerHaptic(45)
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

    fun pollStats() {
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

            _liveStats.value = LiveGameStats(
                score = currentScore,
                distanceMeters = physics.distanceTraveled.toInt(),
                ballsDodged = physics.ballsDodged,
                comboMultiplier = physics.comboMultiplier,
                comboProgress = if (physics.comboTimer > 0f) (physics.comboTimer / physics.maxComboTimer).coerceIn(0f, 1f) else 0f,
                forwardSpeed = physics.player.forwardSpeed * physics.currentSector.speedMultiplier,
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
                timeOfDayCycleProgress = tod.cycleProgress
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
                totalWalletPoints = updatedProfile.totalPoints
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

    fun triggerHaptic(durationMs: Long) {
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        durationMs,
                        VibrationEffect.DEFAULT_AMPLITUDE
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
}

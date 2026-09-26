package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameDatabase
import com.example.data.GameRecord
import com.example.data.GameRecordRepository
import com.example.engine.GameAudio
import com.example.engine.GamePhysicsEngine
import com.example.engine.GameRenderer
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
    val forwardSpeed: Float = 10f,
    val isBraking: Boolean = false,
    val isGrounded: Boolean = true
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRecordRepository
    val audio = GameAudio()

    private val _currentScreen = MutableStateFlow(AppScreen.MENU)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _liveStats = MutableStateFlow(LiveGameStats())
    val liveStats: StateFlow<LiveGameStats> = _liveStats.asStateFlow()

    private val _lastGameOverStats = MutableStateFlow(Triple(0, 0, 0)) // score, dist, dodged
    val lastGameOverStats: StateFlow<Triple<Int, Int, Int>> = _lastGameOverStats.asStateFlow()

    private val _isNewHighScore = MutableStateFlow(false)
    val isNewHighScore: StateFlow<Boolean> = _isNewHighScore.asStateFlow()

    private val _characterColor = MutableStateFlow("Classic Blue")
    val characterColor: StateFlow<String> = _characterColor.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    val topRecord: StateFlow<GameRecord?>
    val allRecords: StateFlow<List<GameRecord>>

    val physics: GamePhysicsEngine
    val renderer: GameRenderer

    init {
        val db = GameDatabase.getDatabase(application)
        repository = GameRecordRepository(db.gameRecordDao())

        topRecord = repository.topRecord.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

        allRecords = repository.allRecords.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        physics = GamePhysicsEngine(audio) { score, distance, dodged ->
            onGameEnded(score, distance, dodged)
        }

        renderer = GameRenderer(physics)
    }

    fun startGame() {
        _isNewHighScore.value = false
        physics.startNewGame(_characterColor.value)
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
        _currentScreen.value = AppScreen.MENU
    }

    fun restartGame() {
        startGame()
    }

    fun setCharacterColor(colorPreset: String) {
        _characterColor.value = colorPreset
        physics.player.applyColorPreset(colorPreset)
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
        triggerHaptic(50)
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
            _liveStats.value = LiveGameStats(
                score = physics.score,
                distanceMeters = physics.distanceTraveled.toInt(),
                ballsDodged = physics.ballsDodged,
                forwardSpeed = physics.player.forwardSpeed,
                isBraking = renderer.isBrakeHeld,
                isGrounded = physics.player.isGrounded
            )
        }
    }

    private fun onGameEnded(score: Int, distance: Int, dodged: Int) {
        _lastGameOverStats.value = Triple(score, distance, dodged)
        triggerHaptic(250)

        viewModelScope.launch {
            val previousBest = topRecord.value?.score ?: 0
            val isNewBest = score > previousBest && score > 0
            _isNewHighScore.value = isNewBest
            if (isNewBest) {
                audio.playHighScoreChime()
            }

            repository.saveRecord(
                GameRecord(
                    score = score,
                    distanceMeters = distance,
                    ballsDodged = dodged,
                    characterColor = _characterColor.value
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

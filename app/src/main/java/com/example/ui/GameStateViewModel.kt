package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Represents the complete game state for Compose UI consumption.
 */
data class GameState(
    val currentScore: Int = 0,
    val isGameOver: Boolean = false,
    val gameSpeed: Float = 10.0f,
    val isPaused: Boolean = false,
    val isPlaying: Boolean = false,
    val distanceMeters: Int = 0,
    val ballsDodged: Int = 0,
    val comboMultiplier: Int = 1,
    val sectorName: String = "SECTOR 1",
    val speedMultiplier: Float = 1.0f,
    val elapsedTimeSeconds: Float = 0.0f
) {
    // Convenience getters for flexible naming conventions
    val score: Int get() = currentScore
    val gameOverStatus: Boolean get() = isGameOver
    val speed: Float get() = gameSpeed
}

/**
 * ViewModel to manage the game state, including current score, game-over status,
 * and game speed, using StateFlow for Compose UI updates, powered by a frame-independent
 * Coroutine game loop.
 */
open class GameStateViewModel : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _currentScore = MutableStateFlow(0)
    val currentScore: StateFlow<Int> = _currentScore.asStateFlow()

    private val _isGameOver = MutableStateFlow(false)
    val isGameOver: StateFlow<Boolean> = _isGameOver.asStateFlow()
    val gameOverStatus: StateFlow<Boolean> = _isGameOver

    private val _gameSpeed = MutableStateFlow(10.0f)
    val gameSpeed: StateFlow<Float> = _gameSpeed.asStateFlow()

    // Configurable speed increment parameters for frame-independent scaling
    var baseSpeed: Float = 10.0f
    var speedIncrementRate: Float = 0.10f // Smoothed speed increment in m/s per second survived
    var maxGameSpeed: Float = 40.0f
    var scoreMultiplier: Float = 2.0f

    private var gameLoopJob: Job? = null
    val isLoopRunning: Boolean
        get() = gameLoopJob?.isActive == true

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
            stopGameLoop()
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

    /**
     * Starts a frame-independent game loop using a Coroutine that continuously
     * updates the game state in the ViewModel, accounting for speed increments.
     */
    fun startGameLoop(tickDelayMs: Long = 16L) {
        stopGameLoop()
        _isGameOver.value = false
        _gameState.update { it.copy(isPlaying = true, isPaused = false, isGameOver = false) }

        gameLoopJob = viewModelScope.launch {
            var lastTimeNanos = System.nanoTime()
            var distanceAcc = _gameState.value.distanceMeters.toFloat()
            var elapsedSec = _gameState.value.elapsedTimeSeconds
            var scoreAcc = _currentScore.value.toFloat()

            while (isActive && !_isGameOver.value) {
                val now = System.nanoTime()
                // Compute frame-independent delta time (dt in seconds)
                val dt = if (lastTimeNanos != 0L) {
                    ((now - lastTimeNanos) / 1_000_000_000.0f).coerceIn(0.001f, 0.1f)
                } else {
                    0.016f
                }
                lastTimeNanos = now

                if (!_gameState.value.isPaused) {
                    elapsedSec += dt

                    // Dynamic speed increment calculation based on elapsed time and acceleration rate
                    val currentSpd = _gameSpeed.value
                    val newSpeed = (currentSpd + speedIncrementRate * dt).coerceAtMost(maxGameSpeed)
                    _gameSpeed.value = newSpeed

                    // Frame-independent distance and score progression based on actual speed
                    distanceAcc += newSpeed * dt
                    val scoreGain = newSpeed * dt * scoreMultiplier
                    scoreAcc += scoreGain
                    val currentScoreInt = scoreAcc.toInt()

                    _currentScore.value = currentScoreInt
                    _gameState.update { current ->
                        current.copy(
                            currentScore = currentScoreInt,
                            gameSpeed = newSpeed,
                            distanceMeters = distanceAcc.toInt(),
                            elapsedTimeSeconds = elapsedSec,
                            speedMultiplier = (newSpeed / baseSpeed).coerceAtLeast(1.0f),
                            isPlaying = true,
                            isGameOver = false
                        )
                    }
                }

                delay(tickDelayMs)
            }
        }
    }

    /**
     * Manually perform a single frame-independent game step by a specified delta time.
     * Ensures deterministic verification in unit tests.
     */
    fun updateGameStep(dt: Float) {
        if (_isGameOver.value || _gameState.value.isPaused) return

        val clampedDt = dt.coerceAtLeast(0.0001f)
        val currentSpd = _gameSpeed.value
        val newSpeed = (currentSpd + speedIncrementRate * clampedDt).coerceAtMost(maxGameSpeed)
        _gameSpeed.value = newSpeed

        val newElapsed = _gameState.value.elapsedTimeSeconds + clampedDt
        val newDistance = _gameState.value.distanceMeters + (newSpeed * clampedDt).toInt()
        val scoreGain = (newSpeed * clampedDt * scoreMultiplier).toInt()
        val newScore = _currentScore.value + scoreGain

        _currentScore.value = newScore
        _gameState.update {
            it.copy(
                currentScore = newScore,
                gameSpeed = newSpeed,
                distanceMeters = newDistance,
                elapsedTimeSeconds = newElapsed,
                speedMultiplier = (newSpeed / baseSpeed).coerceAtLeast(1.0f)
            )
        }
    }

    fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    fun startGame() {
        _isGameOver.value = false
        _currentScore.value = 0
        _gameSpeed.value = baseSpeed
        _gameState.value = GameState(
            currentScore = 0,
            isGameOver = false,
            gameSpeed = baseSpeed,
            isPlaying = true,
            isPaused = false,
            distanceMeters = 0,
            elapsedTimeSeconds = 0.0f
        )
        startGameLoop()
    }

    fun pauseGame() {
        _gameState.update { it.copy(isPaused = true) }
    }

    fun resumeGame() {
        _gameState.update { it.copy(isPaused = false) }
        if (!isLoopRunning) {
            startGameLoop()
        }
    }

    fun resetGame() {
        stopGameLoop()
        startGame()
    }

    override fun onCleared() {
        super.onCleared()
        stopGameLoop()
    }
}

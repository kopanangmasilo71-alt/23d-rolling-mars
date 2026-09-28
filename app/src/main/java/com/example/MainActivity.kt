package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppScreen
import com.example.ui.GameHud
import com.example.ui.GameOverOverlay
import com.example.ui.GameSurfaceView
import com.example.ui.GameViewModel
import com.example.ui.MainMenu
import com.example.ui.PauseOverlay
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = true) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    RollRunnerApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun RollRunnerApp(viewModel: GameViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val shakeTrigger by viewModel.screenShakeTrigger.collectAsStateWithLifecycle()
    val shakeIntensity by viewModel.screenShakeIntensity.collectAsStateWithLifecycle()

    val shakeAnimX = remember { Animatable(0f) }
    val shakeAnimY = remember { Animatable(0f) }
    val shakeRotation = remember { Animatable(0f) }

    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger != 0L && shakeIntensity > 0f) {
            val intensity = shakeIntensity
            val signX = if (Random.nextBoolean()) 1f else -1f
            val signY = if (Random.nextBoolean()) 1f else -1f
            val signRot = if (Random.nextBoolean()) 1f else -1f

            shakeAnimX.snapTo(signX * intensity)
            shakeAnimY.snapTo(signY * intensity * 0.70f)
            shakeRotation.snapTo(signRot * (intensity * 0.05f).coerceAtMost(2.0f))

            coroutineScope {
                launch {
                    shakeAnimX.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.38f,
                            stiffness = 1600f
                        )
                    )
                }
                launch {
                    shakeAnimY.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.38f,
                            stiffness = 1600f
                        )
                    )
                }
                launch {
                    shakeRotation.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.42f,
                            stiffness = 1800f
                        )
                    )
                }
            }
        }
    }

    // Handle back button for sub-screens
    BackHandler(enabled = currentScreen != AppScreen.MENU) {
        when (currentScreen) {
            AppScreen.PLAYING -> viewModel.pauseGame()
            AppScreen.PAUSED -> viewModel.resumeGame()
            AppScreen.GAME_OVER -> viewModel.goToMenu()
            AppScreen.MENU -> { /* default exit */ }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = shakeAnimX.value
                translationY = shakeAnimY.value
                rotationZ = shakeRotation.value
            }
    ) {
        // 3D OpenGL ES Surface View renders the continuous 3D world
        GameSurfaceView(renderer = viewModel.renderer)

        // UI Layer
        when (currentScreen) {
            AppScreen.MENU -> {
                MainMenu(viewModel = viewModel)
            }
            AppScreen.PLAYING -> {
                GameHud(viewModel = viewModel)
            }
            AppScreen.PAUSED -> {
                GameHud(viewModel = viewModel)
                PauseOverlay(viewModel = viewModel)
            }
            AppScreen.GAME_OVER -> {
                GameOverOverlay(viewModel = viewModel)
            }
        }
    }
}

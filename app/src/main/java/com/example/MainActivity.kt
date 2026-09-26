package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppScreen
import com.example.ui.GameHud
import com.example.ui.GameOverOverlay
import com.example.ui.GameSurfaceView
import com.example.ui.GameViewModel
import com.example.ui.MainMenu
import com.example.ui.PauseOverlay
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
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

    // Handle back button for sub-screens
    BackHandler(enabled = currentScreen != AppScreen.MENU) {
        when (currentScreen) {
            AppScreen.PLAYING -> viewModel.pauseGame()
            AppScreen.PAUSED -> viewModel.resumeGame()
            AppScreen.GAME_OVER -> viewModel.goToMenu()
            AppScreen.MENU -> { /* default exit */ }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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

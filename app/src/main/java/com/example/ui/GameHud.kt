package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

@Composable
fun GameHud(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    // Poll stats regularly for HUD numbers
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.pollStats()
            delay(40)
        }
    }

    val stats by viewModel.liveStats.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // TOP HUD BAR
        TopHudBar(
            score = stats.score,
            distance = stats.distanceMeters,
            dodged = stats.ballsDodged,
            speed = stats.forwardSpeed,
            isBraking = stats.isBraking,
            onPauseClick = { viewModel.pauseGame() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // BOTTOM MOBILE TOUCH CONTROLS
        TouchController(
            onLeftChange = { held -> viewModel.setLeftHeld(held) },
            onRightChange = { held -> viewModel.setRightHeld(held) },
            onJump = { viewModel.jump() },
            onBrakeChange = { held -> viewModel.setBrakeHeld(held) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 18.dp)
        )
    }
}

@Composable
private fun TopHudBar(
    score: Int,
    distance: Int,
    dodged: Int,
    speed: Float,
    isBraking: Boolean,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xB0101426),
        tonalElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x40FFFFFF))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Distance Chip
                Column {
                    Text(
                        text = "DISTANCE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF80D8FF),
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "${distance}m",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                // Score Chip
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SCORE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F),
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "$score",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFE082)
                    )
                }

                // Dodged Balls Chip
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "DODGED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB9F6CA),
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "$dodged",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF69F0AE)
                    )
                }

                // Pause Button
                IconButton(
                    onClick = onPauseClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0x33FFFFFF), CircleShape)
                        .testTag("btn_pause")
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause Game",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dynamic Speed / Deceleration Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isBraking) "DECELERATING" else "SPEED",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isBraking) Color(0xFFFF5252) else Color(0xAAFFFFFF)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val speedProgress = ((speed - 3.2f) / (14.5f - 3.2f)).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0x40FFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(speedProgress)
                            .height(4.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        if (isBraking) Color(0xFFFF5252) else Color(0xFF00E5FF),
                                        if (isBraking) Color(0xFFFF1744) else Color(0xFF00E676)
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

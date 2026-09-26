package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainMenu(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val topRecord by viewModel.topRecord.collectAsStateWithLifecycle()
    val allRecords by viewModel.allRecords.collectAsStateWithLifecycle()
    val currentPreset by viewModel.characterColor.collectAsStateWithLifecycle()
    val soundOn by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val vibrationOn by viewModel.vibrationEnabled.collectAsStateWithLifecycle()

    var showHowToPlay by remember { mutableStateOf(false) }
    var showHighScores by remember { mutableStateOf(false) }

    val presets = listOf(
        "Classic Blue" to Color(0xFF1E88E5),
        "Neon Orange" to Color(0xFFFF6D00),
        "Emerald Runner" to Color(0xFF00C853),
        "Cyber Violet" to Color(0xFFAA00FF),
        "Solar Gold" to Color(0xFFFFD600)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xD90A0E1A),
                        Color(0xE6121829),
                        Color(0xF2161E38)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        // TOP CONTROLS (Sound, Vibration, How to Play, Leaderboard)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Trophy / High score chip
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x33FFD54F),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FFD54F)),
                modifier = Modifier
                    .clickable { showHighScores = true }
                    .testTag("btn_show_records")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Leaderboard",
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (topRecord != null) "BEST: ${topRecord?.score}" else "LEADERBOARD",
                        color = Color(0xFFFFD54F),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Action Icons Row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { showHowToPlay = true },
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0x33FFFFFF), CircleShape)
                        .testTag("btn_how_to_play")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "How to play",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleSound() },
                    modifier = Modifier
                        .size(42.dp)
                        .background(if (soundOn) Color(0x3300E5FF) else Color(0x33FFFFFF), CircleShape)
                        .testTag("btn_toggle_sound")
                ) {
                    Icon(
                        imageVector = if (soundOn) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                        contentDescription = "Toggle sound",
                        tint = if (soundOn) Color(0xFF00E5FF) else Color(0x88FFFFFF)
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleVibration() },
                    modifier = Modifier
                        .size(42.dp)
                        .background(if (vibrationOn) Color(0x3300E676) else Color(0x33FFFFFF), CircleShape)
                        .testTag("btn_toggle_vibration")
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = "Toggle vibration",
                        tint = if (vibrationOn) Color(0xFF00E676) else Color(0x88FFFFFF)
                    )
                }
            }
        }

        // CENTER MAIN CONTENT
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stylized Title
            Text(
                text = "ROLL",
                fontSize = 46.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = Color.White
            )
            Text(
                text = "RUNNER",
                fontSize = 46.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp,
                color = Color(0xFF00E5FF)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x2500E5FF)
            ) {
                Text(
                    text = "3D ROLLING BOULDER SURVIVAL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = Color(0xFF80D8FF),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // BIG PLAY BUTTON
            Button(
                onClick = { viewModel.startGame() },
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(64.dp)
                    .testTag("btn_play_game"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF),
                    contentColor = Color(0xFF0A1020)
                ),
                shape = RoundedCornerShape(32.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "START RUN",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // CHARACTER SUIT PRESETS
            Text(
                text = "RUNNER SUIT STYLE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = Color(0xAAFFFFFF)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                presets.forEach { (name, color) ->
                    val isSelected = currentPreset == name
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 46.dp else 38.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else Color(0x44FFFFFF),
                                shape = CircleShape
                            )
                            .clickable { viewModel.setCharacterColor(name) }
                            .testTag("color_preset_${name.replace(" ", "_").lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = currentPreset,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }

        // FOOTER INFO
        Text(
            text = "Smooth analog touch physics • Dynamic obstacles",
            fontSize = 11.sp,
            color = Color(0x66FFFFFF),
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // HOW TO PLAY MODAL
    if (showHowToPlay) {
        Dialog(onDismissRequest = { showHowToPlay = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141A2E)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4400E5FF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "HOW TO PLAY",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00E5FF)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    InstructionRow("LEFT / RIGHT", "Hold to smoothly steer across the road with momentum and banking.", Color(0xFF00E5FF))
                    Spacer(modifier = Modifier.height(10.dp))
                    InstructionRow("BRAKE", "Hold to decelerate forward speed and let fast crossing balls pass.", Color(0xFFFF5252))
                    Spacer(modifier = Modifier.height(10.dp))
                    InstructionRow("JUMP", "Launch into the air to clear standard incoming rolling balls.", Color(0xFFFFD600))
                    Spacer(modifier = Modifier.height(10.dp))
                    InstructionRow("OBSTACLES", "Avoid straight, crossing, bouncing, and giant juggernaut spheres.", Color(0xFFB388FF))

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { showHowToPlay = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("GOT IT!", color = Color(0xFF0A1020), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // HIGH SCORES LEADERBOARD MODAL
    if (showHighScores) {
        Dialog(onDismissRequest = { showHighScores = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141A2E)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFD54F)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "HIGH SCORES",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD54F)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (allRecords.isEmpty()) {
                        Text(
                            text = "No runs yet. Start your first run to record your high score!",
                            fontSize = 13.sp,
                            color = Color(0x88FFFFFF),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            itemsIndexed(allRecords) { idx, item ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (idx == 0) Color(0x33FFD54F) else Color(0x22FFFFFF),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "#${idx + 1}",
                                            fontWeight = FontWeight.Bold,
                                            color = if (idx == 0) Color(0xFFFFD54F) else Color.White,
                                            fontSize = 14.sp
                                        )
                                        Column {
                                            Text(
                                                text = "${item.score} pts",
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = "${item.distanceMeters}m • ${item.ballsDodged} dodged",
                                                color = Color(0xAAFFFFFF),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showHighScores = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("CLOSE", color = Color(0xFF0A1020), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun InstructionRow(title: String, desc: String, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = accentColor.copy(alpha = 0.2f),
            modifier = Modifier.width(92.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = desc,
            fontSize = 12.sp,
            color = Color(0xDDFFFFFF),
            modifier = Modifier.weight(1f)
        )
    }
}

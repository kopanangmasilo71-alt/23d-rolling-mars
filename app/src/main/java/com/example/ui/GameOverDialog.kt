package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun GameOverOverlay(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.gameOverSummary.collectAsStateWithLifecycle()
    val topRecord by viewModel.topRecord.collectAsStateWithLifecycle()
    val currentScore by viewModel.currentScore.collectAsStateWithLifecycle()
    val displayScore = if (summary.score > 0) summary.score else currentScore
    var showHighScoresModal by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xD9060A14))
            .testTag("game_over_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF210162B)),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (summary.isNewHighScore) Color(0xFFFFD54F) else Color(0x3300E5FF)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .widthIn(max = 500.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // New High Score celebration badge
                if (summary.isNewHighScore) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x33FFD54F),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F)),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AppIcons.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "NEW ALL-TIME RECORD!",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD54F),
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                // Title and Rank / Grade Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "GAME OVER",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF5252),
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.testTag("game_over_title")
                        )
                        Text(
                            text = "COLLISION • ${summary.sectorReached}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF),
                            letterSpacing = 1.sp
                        )
                    }

                    // Performance Grade Emblem
                    val gradeColor = when (summary.performanceGrade) {
                        "S" -> Color(0xFFFFD700)
                        "A" -> Color(0xFF00E5FF)
                        "B" -> Color(0xFF00E676)
                        else -> Color(0xFFFF9100)
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color(0x22000000),
                        border = androidx.compose.foundation.BorderStroke(2.dp, gradeColor),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = summary.performanceGrade,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = gradeColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Score Highlight Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x2AFFFFFF),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x33FFFFFF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("final_score_card")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = "FINAL SCORE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F),
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "$displayScore",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.testTag("final_score")
                        )
                        if (!summary.isNewHighScore && (topRecord?.score ?: 0) > 0) {
                            Text(
                                text = "Personal Best: ${topRecord?.score} pts",
                                fontSize = 11.sp,
                                color = Color(0xFFB0BEC5)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Restart Button
                Button(
                    onClick = { viewModel.restartGame() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5FF),
                        contentColor = Color(0xFF0A0E1A)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("restart_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Restart")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RESTART",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats Breakdown Grid
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x18FFFFFF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatLine("Distance Traveled", "${summary.distanceMeters} m", Color(0xFF80D8FF))
                        val mins = summary.runDurationSeconds / 60
                        val secs = summary.runDurationSeconds % 60
                        StatLine("Run Duration", String.format(java.util.Locale.US, "%02d:%02d", mins, secs), Color(0xFFFFD54F))
                        StatLine("Max Threat Level", "LVL ${summary.maxThreatLevelReached}", Color(0xFFFF7043))
                        StatLine("Boulders Dodged", "${summary.ballsDodged}", Color(0xFF81C784))
                        StatLine("Max Combo Multiplier", "${summary.maxCombo}X", Color(0xFFFF80AB))
                        StatLine("Points Earned", "+${summary.pointsEarned} PTS", Color(0xFFFFD54F))
                        StatLine("Pilot Balance", "${summary.totalWalletPoints} PTS", Color(0xFF00E5FF))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Secondary Buttons: Leaderboard & Main Menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.playMenuClick()
                            showHighScoresModal = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF78909C)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("view_leaderboard_button")
                    ) {
                        Icon(imageVector = AppIcons.Leaderboard, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RANKS", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.goToMenu() },
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF78909C)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("home_menu_button")
                    ) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("MENU", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showHighScoresModal) {
        HighScoresDialog(
            viewModel = viewModel,
            onDismiss = { showHighScoresModal = false }
        )
    }
}

@Composable
private fun StatLine(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFFB0BEC5),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor
        )
    }
}

@Composable
fun PauseOverlay(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    var showSettingsModal by remember { mutableStateOf(false) }
    val soundOn by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val vibrationOn by viewModel.vibrationEnabled.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xD9060A14)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF20F1626)),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF)),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .widthIn(max = 440.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PAUSED",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 3.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 1. RESUME Button (Vibrant Emerald Green)
                Button(
                    onClick = { viewModel.resumeGame() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E676),
                        contentColor = Color(0xFF04210D)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("resume_button")
                ) {
                    Text(
                        text = "RESUME",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. RESTART Button (Vibrant Electric Blue)
                Button(
                    onClick = { viewModel.restartGame() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2979FF),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("pause_restart_button")
                ) {
                    Text(
                        text = "RESTART",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. SETTINGS Button (Sleek Slate Blue)
                Button(
                    onClick = {
                        viewModel.playMenuClick()
                        showSettingsModal = !showSettingsModal
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF455A64),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("pause_settings_button")
                ) {
                    Text(
                        text = "SETTINGS",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                }

                // Inline quick toggles if settings expanded
                if (showSettingsModal) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0x66000000),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("SFX Audio", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                androidx.compose.material3.Switch(
                                    checked = soundOn,
                                    onCheckedChange = { viewModel.toggleSound() }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Haptic Feedback", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                androidx.compose.material3.Switch(
                                    checked = vibrationOn,
                                    onCheckedChange = { viewModel.toggleVibration() }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. EXIT Button (Vibrant Coral Red)
                Button(
                    onClick = { viewModel.goToMenu() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5252),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("pause_menu_button")
                ) {
                    Text(
                        text = "EXIT",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }
    }
}


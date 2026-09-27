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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    var showHighScoresModal by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xD9060A14)),
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
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
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
                                imageVector = Icons.Default.EmojiEvents,
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

                // Rank / Grade Badge and Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "RUN TERMINATED",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF5252),
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "${summary.sectorReached} • ${summary.difficultyMode.uppercase()}",
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = "FINAL SCORE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F),
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "${summary.score}",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
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

                Spacer(modifier = Modifier.height(12.dp))

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
                        StatLine("Boulders Dodged", "${summary.ballsDodged}", Color(0xFF81C784))
                        StatLine("Max Combo Multiplier", "${summary.maxCombo}X", Color(0xFFFF80AB))
                        StatLine("Points Earned", "+${summary.pointsEarned} PTS", Color(0xFFFFD54F))
                        StatLine("Pilot Balance", "${summary.totalWalletPoints} PTS", Color(0xFF00E5FF))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Retry Button
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
                        .testTag("retry_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "REPLAY RUN",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Buttons: Leaderboard & Main Menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showHighScoresModal = true },
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF78909C)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("view_leaderboard_button")
                    ) {
                        Icon(imageVector = Icons.Default.Leaderboard, contentDescription = null, modifier = Modifier.size(18.dp))
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xD9060A14)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF210162B)),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "RUN PAUSED",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00E5FF),
                    letterSpacing = 2.sp
                )
                Text(
                    text = "TACTICAL BREAK",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB0BEC5),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Resume Button
                Button(
                    onClick = { viewModel.resumeGame() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5FF),
                        contentColor = Color(0xFF0A0E1A)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("resume_button")
                ) {
                    Text(
                        text = "RESUME RUN",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Restart Button
                OutlinedButton(
                    onClick = { viewModel.restartGame() },
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF78909C)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pause_restart_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESTART", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Menu Button
                OutlinedButton(
                    onClick = { viewModel.goToMenu() },
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF78909C)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pause_menu_button")
                ) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("MAIN MENU", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


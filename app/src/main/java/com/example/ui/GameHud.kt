package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.pollStats()
            delay(35)
        }
    }

    val stats by viewModel.liveStats.collectAsStateWithLifecycle()
    val sectorAlert by viewModel.sectorAnnouncement.collectAsStateWithLifecycle()
    val timeOfDayAlert by viewModel.timeOfDayAnnouncement.collectAsStateWithLifecycle()
    val nearMissFlash by viewModel.nearMissFlash.collectAsStateWithLifecycle()
    val shieldDeflected by viewModel.shieldDeflectedAlert.collectAsStateWithLifecycle()
    val collectiblePickup by viewModel.collectiblePickupAlert.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // TOP HUD BAR
        TopHudBar(
            stats = stats,
            onPauseClick = { viewModel.pauseGame() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )

        // SECTOR TRANSITION ANNOUNCEMENT
        AnimatedVisibility(
            visible = sectorAlert != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 110.dp)
        ) {
            sectorAlert?.let { sector ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xF00D1326),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF)),
                    tonalElevation = 12.dp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ENTERING ${sector.name}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00E5FF),
                                letterSpacing = 2.sp
                            )
                        }
                        Text(
                            text = sector.subtitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // TIME OF DAY PHASE TRANSITION ANNOUNCEMENT
        AnimatedVisibility(
            visible = timeOfDayAlert != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 112.dp)
        ) {
            timeOfDayAlert?.let { alertText ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xF2091222),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFB74D)),
                    tonalElevation = 12.dp,
                    modifier = Modifier.padding(horizontal = 22.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = alertText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFE082),
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // COLLECTIBLE PICKUP TOAST NOTIFICATION
        AnimatedVisibility(
            visible = collectiblePickup != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 96.dp)
        ) {
            collectiblePickup?.let { col ->
                val colColor = when (col) {
                    com.example.engine.CollectibleType.SHIELD -> Color(0xFF00E5FF)
                    com.example.engine.CollectibleType.SPEED_BOOST -> Color(0xFF00E676)
                    com.example.engine.CollectibleType.SCORE_MULTIPLIER -> Color(0xFFFFD54F)
                    com.example.engine.CollectibleType.ENERGY_CELL -> Color(0xFFFF4081)
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xF2081024),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, colColor),
                    tonalElevation = 10.dp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = when (col) {
                                com.example.engine.CollectibleType.SHIELD -> Icons.Default.Security
                                com.example.engine.CollectibleType.SPEED_BOOST -> Icons.Default.Speed
                                com.example.engine.CollectibleType.SCORE_MULTIPLIER -> Icons.Default.Stars
                                com.example.engine.CollectibleType.ENERGY_CELL -> Icons.Default.Bolt
                            },
                            contentDescription = null,
                            tint = colColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${col.displayName} ONLINE!",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = colColor,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = col.description,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // SHIELD DEFLECTION DRAMATIC HERO BANNER
        AnimatedVisibility(
            visible = shieldDeflected,
            enter = scaleIn(tween(150)) + fadeIn(),
            exit = scaleOut(tween(250)) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 96.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xF2001F3F),
                border = androidx.compose.foundation.BorderStroke(2.5.dp, Color(0xFF00E5FF)),
                shadowElevation = 16.dp,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Shield Deflected",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "SHIELD DEFLECTED!",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00E5FF),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "BOULDER BLASTED • RUN SAVED! (+350)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // COMBO MULTIPLIER BADGE (Left center)
        if (stats.comboMultiplier > 1) {
            ComboBadge(
                multiplier = stats.comboMultiplier,
                progress = stats.comboProgress,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 14.dp)
            )
        }

        // NEAR MISS FLASH NOTIFICATION
        AnimatedVisibility(
            visible = nearMissFlash,
            enter = scaleIn(tween(150)) + fadeIn(),
            exit = scaleOut(tween(250)) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 96.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xEE0A192F),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00E5FF)),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CLOSE CALL! +200",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // BOTTOM MOBILE TOUCH CONTROLS
        TouchController(
            onLeftChange = { held -> viewModel.setLeftHeld(held) },
            onRightChange = { held -> viewModel.setRightHeld(held) },
            onJump = { viewModel.jump() },
            onBrakeChange = { held -> viewModel.setBrakeHeld(held) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 14.dp)
        )
    }
}

@Composable
private fun TopHudBar(
    stats: LiveGameStats,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xD90A0F1E),
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300E5FF))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Upper Info Row: Sector & Overdrive & Pause
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sector Tag
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x3300E5FF),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF00E5FF)),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = stats.sectorName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00E5FF),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Time of Day Tag
                    val todColor = when (stats.timeOfDayName) {
                        "DAWN" -> Color(0xFFFF8A65)
                        "MIDDAY" -> Color(0xFFFFD54F)
                        "SUNSET" -> Color(0xFFFF7043)
                        "TWILIGHT" -> Color(0xFFCE93D8)
                        "NIGHT" -> Color(0xFF81D4FA)
                        else -> Color(0xFF00E5FF)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = todColor.copy(alpha = 0.20f),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, todColor.copy(alpha = 0.85f)),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = stats.timeOfDayEmoji,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${stats.timeOfDayName} • ${stats.timeOfDayTime}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = todColor
                            )
                        }
                    }

                    if (stats.isOverdrive) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x44FF3D00),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFFF3D00))
                        ) {
                            Text(
                                text = "OVERDRIVE 2X",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFF5722),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Speed / Turbo Status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (stats.isBoosting) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x4400E676),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF00E676)),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "TURBO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00E676),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "${String.format("%.1f", stats.forwardSpeed)} m/s",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB0BEC5),
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    IconButton(
                        onClick = onPauseClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("pause_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Active Power-ups Status Row (Presented prominently at top just like distance travelled)
            if (stats.hasShield || stats.isScoreBoosted || stats.isBoosting) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (stats.hasShield) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x3300E5FF),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF00E5FF)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = "Shield Active",
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "SHIELD",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF00E5FF),
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    Text(
                                        text = "${stats.shieldRemainingSec}s",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { stats.shieldProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = Color(0xFF00E5FF),
                                    trackColor = Color(0x3300E5FF)
                                )
                            }
                        }
                    }

                    if (stats.isScoreBoosted) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x33FFD54F),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFFFD54F)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Stars,
                                            contentDescription = "Score Multiplier Active",
                                            tint = Color(0xFFFFD54F),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${stats.scoreMultiplierValue}X SCORE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFFFD54F),
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    Text(
                                        text = "${stats.scoreMultiplierRemainingSec}s",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { stats.scoreMultiplierProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = Color(0xFFFFD54F),
                                    trackColor = Color(0x33FFD54F)
                                )
                            }
                        }
                    }

                    if (stats.isBoosting) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x3300E676),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF00E676)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Speed,
                                            contentDescription = "Hyper Boost Active",
                                            tint = Color(0xFF00E676),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "BOOST",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF00E676),
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    Text(
                                        text = "${stats.boostRemainingSec}s",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { stats.boostProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = Color(0xFF00E676),
                                    trackColor = Color(0x3300E676)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Stats Row: Distance, Active Power, Score & Dodges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Distance
                Column {
                    Text(
                        text = "DISTANCE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF90CAF9),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${stats.distanceMeters}m",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                // Active Power (Presented at the top just like distance travelled)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ACTIVE POWER",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            stats.hasShield -> Color(0xFF00E5FF)
                            stats.isBoosting -> Color(0xFF00E676)
                            stats.isScoreBoosted -> Color(0xFFFFD54F)
                            else -> Color(0xFF78909C)
                        },
                        letterSpacing = 1.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when {
                            stats.hasShield -> {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Shield Active",
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "SHIELD ${stats.shieldRemainingSec}s",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF00E5FF)
                                )
                            }
                            stats.isBoosting -> {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Hyper Boost Active",
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "BOOST ${stats.boostRemainingSec}s",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF00E676)
                                )
                            }
                            stats.isScoreBoosted -> {
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = "Score Multiplier Active",
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${stats.scoreMultiplierValue}X (${stats.scoreMultiplierRemainingSec}s)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFFD54F)
                                )
                            }
                            else -> {
                                Text(
                                    text = "NONE",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF546E7A)
                                )
                            }
                        }
                    }
                }

                // Score
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SCORE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${stats.score}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = if (stats.hasBeatenHighScore) Color(0xFFFFD54F) else Color.White
                    )
                }

                // Dodged
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "DODGED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF81C784),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${stats.ballsDodged}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            // Real-time High Score Chase Bar
            if (stats.highScoreToBeat > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                val progress = if (stats.highScoreToBeat > 0) {
                    (stats.score.toFloat() / stats.highScoreToBeat.toFloat()).coerceIn(0f, 1f)
                } else 1f

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = if (stats.hasBeatenHighScore) Color(0xFFFFD54F) else Color(0xFF78909C),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (stats.hasBeatenHighScore) Color(0xFFFFD54F) else Color(0xFF00E5FF),
                        trackColor = Color(0x33FFFFFF)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (stats.hasBeatenHighScore) "NEW RECORD!" else "BEST: ${stats.highScoreToBeat}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (stats.hasBeatenHighScore) Color(0xFFFFD54F) else Color(0xFFB0BEC5)
                    )
                }
            }
        }
    }
}

@Composable
private fun ComboBadge(
    multiplier: Int,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val (badgeColor, glowColor) = when (multiplier) {
        2 -> Color(0xFF00E5FF) to Color(0x6600E5FF)
        3 -> Color(0xFFFFD600) to Color(0x66FFD600)
        4 -> Color(0xFFFF6D00) to Color(0x66FF6D00)
        else -> Color(0xFFFF1744) to Color(0x66FF1744)
    }

    Surface(
        modifier = modifier
            .scale(pulseScale)
            .testTag("combo_badge"),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xF20A1024),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, badgeColor),
        shadowElevation = 8.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = "COMBO",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = badgeColor,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "${multiplier}X",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(48.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = badgeColor,
                trackColor = Color(0x22FFFFFF)
            )
        }
    }
}

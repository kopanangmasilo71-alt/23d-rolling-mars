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
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GameHud(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        if (!viewModel.isGameLoopRunning) {
            viewModel.startGameLoop()
        }
    }

    val stats by viewModel.liveStats.collectAsStateWithLifecycle()
    val currentScore by viewModel.currentScore.collectAsStateWithLifecycle()
    val gameSpeed by viewModel.gameSpeed.collectAsStateWithLifecycle()
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val sectorAlert by viewModel.sectorAnnouncement.collectAsStateWithLifecycle()
    val threatAlert by viewModel.threatEscalationAnnouncement.collectAsStateWithLifecycle()
    val timeOfDayAlert by viewModel.timeOfDayAnnouncement.collectAsStateWithLifecycle()
    val nearMissFlash by viewModel.nearMissFlash.collectAsStateWithLifecycle()
    val shieldDeflected by viewModel.shieldDeflectedAlert.collectAsStateWithLifecycle()
    val collectiblePickup by viewModel.collectiblePickupAlert.collectAsStateWithLifecycle()

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val alertTopPadding = if (isLandscape) 46.dp else 68.dp

    val shakeTrigger by viewModel.screenShakeTrigger.collectAsStateWithLifecycle()
    val shakeIntensity by viewModel.screenShakeIntensity.collectAsStateWithLifecycle()
    val shakeAnimX = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
    val shakeAnimY = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger != 0L && shakeIntensity > 0f) {
            val intensity = shakeIntensity
            val signX = if (kotlin.random.Random.nextBoolean()) 1f else -1f
            val signY = if (kotlin.random.Random.nextBoolean()) 1f else -1f
            shakeAnimX.snapTo(signX * intensity)
            shakeAnimY.snapTo(signY * intensity * 0.75f)
            kotlinx.coroutines.coroutineScope {
                launch {
                    shakeAnimX.animateTo(
                        targetValue = 0f,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = 0.35f,
                            stiffness = 1400f
                        )
                    )
                }
                launch {
                    shakeAnimY.animateTo(
                        targetValue = 0f,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = 0.35f,
                            stiffness = 1400f
                        )
                    )
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .graphicsLayer {
                translationX = shakeAnimX.value
                translationY = shakeAnimY.value
            }
    ) {
        // TOP HUD BAR - Compact, space-optimized floating cyber HUD
        TopHudBar(
            stats = stats,
            currentScore = currentScore,
            gameSpeed = gameSpeed,
            onPauseClick = { viewModel.pauseGame() },
            isLandscape = isLandscape,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = if (isLandscape) 18.dp else 12.dp, vertical = 4.dp)
        )

        // SECTOR TRANSITION ANNOUNCEMENT
        AnimatedVisibility(
            visible = sectorAlert != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = alertTopPadding)
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
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = AppIcons.Bolt,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ENTERING ${sector.name}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00E5FF),
                                letterSpacing = 2.sp
                            )
                        }
                        Text(
                            text = sector.subtitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // DYNAMIC THREAT LEVEL ESCALATION BANNER
        AnimatedVisibility(
            visible = threatAlert != null,
            enter = scaleIn(tween(180)) + fadeIn(),
            exit = scaleOut(tween(250)) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = alertTopPadding)
        ) {
            threatAlert?.let { threat ->
                val badgeColor = Color(threat.badgeColorHex)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xF21C0A0A),
                    border = androidx.compose.foundation.BorderStroke(1.8.dp, badgeColor),
                    shadowElevation = 16.dp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Threat Escalation",
                            tint = badgeColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "THREAT ESCALATION • LVL ${threat.level}: ${threat.name}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = badgeColor,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${threat.description} (BOULDERS FASTER & DENSER)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
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
                .padding(top = alertTopPadding)
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
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = alertText,
                            fontSize = 14.sp,
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
                .padding(top = alertTopPadding)
        ) {
            collectiblePickup?.let { col ->
                val colColor = when (col) {
                    com.example.engine.CollectibleType.SHIELD -> Color(0xFF00E5FF)
                    com.example.engine.CollectibleType.SPEED_BOOST -> Color(0xFF00E676)
                    com.example.engine.CollectibleType.SCORE_MULTIPLIER -> Color(0xFFFFD54F)
                    com.example.engine.CollectibleType.ENERGY_CELL -> Color(0xFFFF4081)
                    com.example.engine.CollectibleType.ENERGY_ORB -> Color(0xFFFFD700)
                    com.example.engine.CollectibleType.AMMO_PACK -> Color(0xFFFF6D00)
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
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = when (col) {
                                com.example.engine.CollectibleType.SHIELD -> AppIcons.Security
                                com.example.engine.CollectibleType.SPEED_BOOST -> AppIcons.Speed
                                com.example.engine.CollectibleType.SCORE_MULTIPLIER -> AppIcons.Stars
                                com.example.engine.CollectibleType.ENERGY_CELL -> AppIcons.Bolt
                                com.example.engine.CollectibleType.ENERGY_ORB -> AppIcons.Stars
                                com.example.engine.CollectibleType.AMMO_PACK -> AppIcons.FlashOn
                            },
                            contentDescription = null,
                            tint = colColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${col.displayName} ONLINE!",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = colColor,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = col.description,
                                fontSize = 9.sp,
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
                .padding(top = alertTopPadding)
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Security,
                        contentDescription = "Shield Deflected",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SHIELD DEFLECTED!",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00E5FF),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "BOULDER BLASTED • RUN SAVED! (+350)",
                            fontSize = 10.sp,
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
                    .padding(start = if (isLandscape) 20.dp else 14.dp)
            )
        }

        // NEAR MISS FLASH NOTIFICATION
        AnimatedVisibility(
            visible = nearMissFlash,
            enter = scaleIn(tween(150)) + fadeIn(),
            exit = scaleOut(tween(250)) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = alertTopPadding)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xEE0A192F),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD700)),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Bolt,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NEAR MISS! +100",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD700),
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // BOTTOM MOBILE TOUCH CONTROLS
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = if (isLandscape) 4.dp else 10.dp)
        ) {
            // Out of Ammo / Low Ammo Warning Alert
            if (stats.ammo == 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xD9B71C1C),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFFF5252)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(bottom = if (isLandscape) 82.dp else 88.dp)
                ) {
                    Text(
                        text = "⚡ OUT OF AMMO! COLLECT POWER-UPS TO RECHARGE",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            } else if (stats.ammo <= 3) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xD9E65100),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(bottom = if (isLandscape) 82.dp else 88.dp)
                ) {
                    Text(
                        text = "⚡ LOW AMMO: ${stats.ammo} BOLTS LEFT",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            TouchController(
                onLeftChange = { held -> viewModel.setLeftHeld(held) },
                onRightChange = { held -> viewModel.setRightHeld(held) },
                onJump = { viewModel.jump() },
                onShootChange = { held -> viewModel.setShootHeld(held) },
                onShoot = { viewModel.shoot() },
                ammo = stats.ammo,
                isLandscape = isLandscape,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TopHudBar(
    stats: LiveGameStats,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier,
    currentScore: Int = stats.score,
    gameSpeed: Float = stats.forwardSpeed,
    isLandscape: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xE60A0F1E),
        tonalElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300E5FF))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLandscape) {
                // LANDSCAPE: Ultra-compact, single-line cyber bar (~40dp) maximizing game visibility!
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT: Sector & Threat Level & Time & Active Power
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x3300E5FF),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF00E5FF))
                        ) {
                            Text(
                                text = stats.sectorName,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00E5FF),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        val threatColor = Color(stats.threatLevelColorHex)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = threatColor.copy(alpha = 0.22f),
                            border = androidx.compose.foundation.BorderStroke(0.6.dp, threatColor)
                        ) {
                            Text(
                                text = "LVL ${stats.threatLevel} • ${String.format(java.util.Locale.US, "%.1fx", stats.dynamicSpeedMultiplier)}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = threatColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (stats.isBreatherWave) Color(0x3300E676) else Color(0x33FF9100),
                            border = androidx.compose.foundation.BorderStroke(
                                0.6.dp,
                                if (stats.isBreatherWave) Color(0xFF00E676) else Color(0xFFFF9100)
                            )
                        ) {
                            Text(
                                text = if (stats.isBreatherWave) "BREATHER" else "WAVE ${stats.waveIndex} (${String.format(java.util.Locale.US, "%.1fs", stats.currentSpawnInterval)})",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = if (stats.isBreatherWave) Color(0xFF00E676) else Color(0xFFFF9100),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x22FFFFFF),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x44FFFFFF))
                        ) {
                            Text(
                                text = "${stats.timeOfDayEmoji} ${stats.timeOfDayName}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        ActivePowerPill(stats = stats)
                    }

                    // CENTER: Prominent Score, Distance & Dodged Balls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "SCORE ",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD54F),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "$currentScore",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (stats.hasBeatenHighScore) Color(0xFFFFD54F) else Color.White
                            )
                        }

                        Text(
                            text = "•",
                            fontSize = 10.sp,
                            color = Color(0x66FFFFFF)
                        )

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "DIST ",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF90CAF9)
                            )
                            Text(
                                text = "${stats.distanceMeters}m",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "•",
                            fontSize = 10.sp,
                            color = Color(0x66FFFFFF)
                        )

                        Text(
                            text = "🎯 ${stats.ballsDodged}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF81C784)
                        )
                    }

                    // RIGHT: Speed, Ammo, Pause Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.1f", gameSpeed)} m/s",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB0BEC5)
                        )

                        val ammoBadgeColor = if (stats.ammo > 5) Color(0xFFFF6D00) else if (stats.ammo > 0) Color(0xFFFFAB00) else Color(0xFFFF5252)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ammoBadgeColor.copy(alpha = 0.22f),
                            border = androidx.compose.foundation.BorderStroke(0.6.dp, ammoBadgeColor)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.FlashOn,
                                    contentDescription = "Ammo",
                                    tint = ammoBadgeColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${stats.ammo}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ammoBadgeColor
                                )
                            }
                        }

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
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            } else {
                // PORTRAIT: Sleek 2-tier compact cyber bar (~50dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    // Row 1: Sector / Time on Left, Score in Center, Ammo & Pause on Right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0x3300E5FF),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF00E5FF))
                            ) {
                                Text(
                                    text = stats.sectorName,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF00E5FF),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }

                            Text(
                                text = stats.timeOfDayEmoji,
                                fontSize = 10.sp
                            )
                        }

                        // Central Score
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "SCORE ",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD54F),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "$currentScore",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (stats.hasBeatenHighScore) Color(0xFFFFD54F) else Color.White
                            )
                        }

                        // Right: Ammo & Pause
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val ammoBadgeColor = if (stats.ammo > 5) Color(0xFFFF6D00) else if (stats.ammo > 0) Color(0xFFFFAB00) else Color(0xFFFF5252)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ammoBadgeColor.copy(alpha = 0.22f),
                                border = androidx.compose.foundation.BorderStroke(0.6.dp, ammoBadgeColor)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = AppIcons.FlashOn,
                                        contentDescription = "Ammo",
                                        tint = ammoBadgeColor,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${stats.ammo}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = ammoBadgeColor
                                    )
                                }
                            }

                            IconButton(
                                onClick = onPauseClick,
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("pause_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Row 2: Secondary stats (Distance, Active Power, Speed)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${stats.distanceMeters}m",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "• LVL ${stats.threatLevel}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(stats.threatLevelColorHex)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "• 🎯 ${stats.ballsDodged}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF81C784)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (stats.isBreatherWave) Color(0x3300E676) else Color(0x33FF9100),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp,
                                    if (stats.isBreatherWave) Color(0xFF00E676) else Color(0xFFFF9100)
                                )
                            ) {
                                Text(
                                    text = if (stats.isBreatherWave) "BREATHER" else "WAVE ${stats.waveIndex}",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (stats.isBreatherWave) Color(0xFF00E676) else Color(0xFFFF9100),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        // Compact Powerup chip if active
                        ActivePowerPill(stats = stats)

                        Text(
                            text = "${String.format(java.util.Locale.US, "%.1f", gameSpeed)} m/s",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB0BEC5)
                        )
                    }
                }
            }

            // Real-time hairline high score progress line along bottom of HUD bar
            if (stats.highScoreToBeat > 0) {
                val progress = (stats.score.toFloat() / stats.highScoreToBeat.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = if (stats.hasBeatenHighScore) Color(0xFFFFD54F) else Color(0xFF00E5FF),
                    trackColor = Color(0x22FFFFFF)
                )
            }
        }
    }
}

@Composable
private fun ActivePowerPill(stats: LiveGameStats) {
    when {
        stats.hasShield -> {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0x3300E5FF),
                border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFF00E5FF))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Security,
                        contentDescription = "Shield Active",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "SHIELD ${stats.shieldRemainingSec}s",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00E5FF)
                    )
                }
            }
        }
        stats.isBoosting -> {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0x3300E676),
                border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFF00E676))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Speed,
                        contentDescription = "Boost Active",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "BOOST ${stats.boostRemainingSec}s",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00E676)
                    )
                }
            }
        }
        stats.isScoreBoosted -> {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0x33FFD54F),
                border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFFFFD54F))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Stars,
                        contentDescription = "Multiplier Active",
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${stats.scoreMultiplierValue}X (${stats.scoreMultiplierRemainingSec}s)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD54F)
                    )
                }
            }
        }
        stats.orbsCollected > 0 && !stats.isBoosting -> {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0x33FFD700),
                border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFFFFD700))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Bolt,
                        contentDescription = "Orbs",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "ORBS ${stats.orbsCollected}/${stats.maxOrbsForBoost}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD700)
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

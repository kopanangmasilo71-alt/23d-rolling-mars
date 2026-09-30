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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.engine.CharacterModelId
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainMenu(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val topRecord by viewModel.topRecord.collectAsStateWithLifecycle()
    val currentPreset by viewModel.characterColor.collectAsStateWithLifecycle()
    val isOverdrive by viewModel.isOverdriveMode.collectAsStateWithLifecycle()
    val soundOn by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val bgmOn by viewModel.bgmEnabled.collectAsStateWithLifecycle()
    val vibrationOn by viewModel.vibrationEnabled.collectAsStateWithLifecycle()
    val profile by viewModel.playerProfile.collectAsStateWithLifecycle()
    val career by viewModel.careerStats.collectAsStateWithLifecycle()

    var showHowToPlay by remember { mutableStateOf(false) }
    var showHighScores by remember { mutableStateOf(false) }
    var showCharacterHangar by remember { mutableStateOf(false) }

    val walletPoints = profile?.totalPoints ?: 0
    val equippedId = profile?.equippedCharacterId ?: "vanguard"
    val equippedModel = CharacterModelId.fromId(equippedId)
    val unlockedSet = (profile?.unlockedCharacterIds ?: "vanguard").split(",").map { it.trim().lowercase() }.toSet()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    val primaryAccent = if (isOverdrive) Color(0xFFFF3D00) else Color(0xFF00E5FF)
    val secondaryAccent = if (isOverdrive) Color(0xFFFF9100) else Color(0xFF00B0FF)
    val accentGlow = if (isOverdrive) Color(0x66FF3D00) else Color(0x6600E5FF)

    val modelAccentColor = when (equippedModel) {
        CharacterModelId.TITAN -> Color(0xFFFF7043)
        CharacterModelId.VALKYRIE -> Color(0xFF00E676)
        CharacterModelId.PHANTOM -> Color(0xFFE040FB)
        CharacterModelId.CHRONOS -> Color(0xFFFFD54F)
        CharacterModelId.VANGUARD -> Color(0xFF00E5FF)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.0f to Color(0xF4040916),
                    0.18f to Color(0xDD070F24),
                    0.46f to Color(0x66081228),
                    0.74f to Color(0xDD050A1A),
                    1.0f to Color(0xF8030610)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // TOP UTILITY BAR (High Score, Points Wallet, and Quick Controls)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // High score & Credits Wallet Chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // High Score Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x28FFD54F),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FFD54F)),
                    modifier = Modifier
                        .clickable {
                            viewModel.playMenuClick()
                            showHighScores = true
                        }
                        .testTag("btn_show_records")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.EmojiEvents,
                            contentDescription = "Leaderboard",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (topRecord != null) "${topRecord?.score}" else "TOP 0",
                            color = Color(0xFFFFD54F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Points Wallet Pill (Tappable to launch Hangar)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x2800E5FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6600E5FF)),
                    modifier = Modifier
                        .clickable {
                            viewModel.playMenuSelect()
                            showCharacterHangar = true
                        }
                        .testTag("btn_wallet_hangar")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.Stars,
                            contentDescription = "Credits",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "$walletPoints PTS",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Quick Actions Cluster
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                // How to play guide
                Surface(
                    shape = CircleShape,
                    color = Color(0x22FFFFFF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable {
                            viewModel.playMenuClick()
                            showHowToPlay = true
                        }
                        .testTag("btn_how_to_play")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = AppIcons.HelpOutline,
                            contentDescription = "How to play",
                            tint = Color(0xEEFFFFFF),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                // Sound toggle
                Surface(
                    shape = CircleShape,
                    color = if (soundOn) Color(0x2A00E5FF) else Color(0x22FFFFFF),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (soundOn) Color(0x8800E5FF) else Color(0x33FFFFFF)
                    ),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable { viewModel.toggleSound() }
                        .testTag("btn_toggle_sound")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (soundOn) AppIcons.VolumeUp else AppIcons.VolumeMute,
                            contentDescription = "Toggle sound",
                            tint = if (soundOn) Color(0xFF00E5FF) else Color(0x77FFFFFF),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                // Synth BGM Music Toggle
                Surface(
                    shape = CircleShape,
                    color = if (bgmOn) Color(0x2AFF9100) else Color(0x22FFFFFF),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (bgmOn) Color(0x88FF9100) else Color(0x33FFFFFF)
                    ),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable { viewModel.toggleBgm() }
                        .testTag("btn_toggle_bgm")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = AppIcons.Bolt,
                            contentDescription = "Toggle Synth Music",
                            tint = if (bgmOn) Color(0xFFFF9100) else Color(0x77FFFFFF),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                // Vibration toggle
                Surface(
                    shape = CircleShape,
                    color = if (vibrationOn) Color(0x2A00E676) else Color(0x22FFFFFF),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (vibrationOn) Color(0x8800E676) else Color(0x33FFFFFF)
                    ),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable { viewModel.toggleVibration() }
                        .testTag("btn_toggle_vibration")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = AppIcons.Vibration,
                            contentDescription = "Toggle vibration",
                            tint = if (vibrationOn) Color(0xFF00E676) else Color(0x77FFFFFF),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }

        // CENTER MAIN CONTENT COLUMN
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Futuristic Game Logo Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ROLL",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "RUNNER",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 5.sp,
                    color = primaryAccent
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Subtitle Tag with pulsing accent dot
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = primaryAccent.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, primaryAccent.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(primaryAccent.copy(alpha = pulseGlow))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isOverdrive) "⚡ OVERDRIVE ACTIVE • 2X SCORE MULTIPLIER" else "3D BOULDER SURVIVAL • SYSTEM V2.4",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = if (isOverdrive) Color(0xFFFFAB91) else Color(0xFF80D8FF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // DIFFICULTY MODE SELECTOR (Standard vs Overdrive 2X)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0x30101C36),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF)),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Standard Mode Tab
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (!isOverdrive) Color(0xFF00E5FF) else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setOverdriveMode(false) }
                            .testTag("mode_standard")
                    ) {
                        Text(
                            text = "STANDARD (1X)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = if (!isOverdrive) Color(0xFF070E1C) else Color(0xFF90A4AE),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    // Overdrive Mode Tab
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isOverdrive) Color(0xFFFF3D00) else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setOverdriveMode(true) }
                            .testTag("mode_overdrive")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Bolt,
                                contentDescription = null,
                                tint = if (isOverdrive) Color.White else Color(0xFFFF7043),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "OVERDRIVE 2X",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = if (isOverdrive) Color.White else Color(0xFFFF7043)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // HERO PLAY BUTTON
            Button(
                onClick = { viewModel.startGame() },
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(58.dp)
                    .testTag("btn_play_game"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, primaryAccent.copy(alpha = pulseGlow)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(primaryAccent, secondaryAccent)
                            ),
                            shape = RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isOverdrive) Color.White else Color(0xFF070E1C),
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = if (isOverdrive) "LAUNCH OVERDRIVE" else "START RUN",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                                color = if (isOverdrive) Color.White else Color(0xFF070E1C)
                            )
                            Text(
                                text = if (isOverdrive) "2X SCORE & HIGH SPEED" else "READY FOR DEPLOYMENT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = if (isOverdrive) Color(0xDDFFFFFF) else Color(0xCC070E1C)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ACTIVE PILOT CHASSIS & ARMORY CARD
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0x380F1A35),
                border = androidx.compose.foundation.BorderStroke(1.dp, modelAccentColor.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .clickable {
                        viewModel.playMenuSelect()
                        showCharacterHangar = true
                    }
                    .testTag("card_pilot_hangar")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pilot Card Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(modelAccentColor.copy(alpha = 0.25f))
                                    .border(1.dp, modelAccentColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = modelAccentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = equippedModel.displayName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = modelAccentColor
                                )
                                Text(
                                    text = equippedModel.title.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = Color(0xAAFFFFFF)
                                )
                            }
                        }

                        // Open Hangar button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x2800E5FF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x5500E5FF))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = AppIcons.ShoppingBag,
                                    contentDescription = "Hangar Shop",
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "HANGAR",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF00E5FF)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Chassis Stat Ratings (Armor, Speed, Tech)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RatingIndicator(label = "ARMOR", rating = equippedModel.armorRating, accent = modelAccentColor)
                        RatingIndicator(label = "SPEED", rating = equippedModel.speedRating, accent = modelAccentColor)
                        RatingIndicator(label = "TECH", rating = equippedModel.techRating, accent = modelAccentColor)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Perk Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x22FFFFFF),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚡ ${equippedModel.perkName}: ${equippedModel.perkDescription}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFCFD8DC),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Character Switch Carousel (LazyRow for zero clipping on all screen sizes)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(CharacterModelId.entries) { model ->
                            val isSelected = equippedModel == model
                            val isUnlocked = unlockedSet.contains(model.id.lowercase())
                            val modelColor = when (model) {
                                CharacterModelId.VANGUARD -> Color(0xFF00E5FF)
                                CharacterModelId.TITAN -> Color(0xFFFF7043)
                                CharacterModelId.VALKYRIE -> Color(0xFF00E676)
                                CharacterModelId.PHANTOM -> Color(0xFFE040FB)
                                CharacterModelId.CHRONOS -> Color(0xFFFFD54F)
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) modelColor.copy(alpha = 0.32f) else Color(0x1AFFFFFF),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) modelColor else Color(0x25FFFFFF)
                                ),
                                modifier = Modifier
                                    .clickable {
                                        if (isUnlocked) {
                                            viewModel.selectOrPurchaseCharacter(model)
                                        } else {
                                            showCharacterHangar = true
                                        }
                                    }
                                    .testTag("quick_select_${model.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(modelColor)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = model.displayName.split(" ").first(),
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xBBFFFFFF)
                                    )
                                    if (!isUnlocked) {
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = Color(0xFFFFD54F),
                                            modifier = Modifier.size(9.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // BOTTOM CAREER TELEMETRY STRIP & CONTROLS FOOTER
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Career Stats Telemetry Pod
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0x28060D1E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x25FFFFFF)),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "DISTANCE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0x88FFFFFF)
                        )
                        Text(
                            text = "${career.totalDistance}m",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF80D8FF)
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(18.dp).background(Color(0x33FFFFFF)))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "DODGED",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0x88FFFFFF)
                        )
                        Text(
                            text = "${career.totalDodged}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00E676)
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(18.dp).background(Color(0x33FFFFFF)))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "BEST COMBO",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0x88FFFFFF)
                        )
                        Text(
                            text = "${career.maxComboEver}x",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD54F)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Touch Controls: Analog Steer • Jump & Brake • Plasma Blaster",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0x55FFFFFF)
            )
        }
    }

    // CHARACTER HANGAR & 3D ROSTER MODAL
    if (showCharacterHangar) {
        CharacterHangarDialog(
            viewModel = viewModel,
            onDismiss = {
                viewModel.playMenuBack()
                showCharacterHangar = false
            }
        )
    }

    // HOW TO PLAY MODAL
    if (showHowToPlay) {
        Dialog(onDismissRequest = {
            viewModel.playMenuBack()
            showHowToPlay = false
        }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10172D)),
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
                        text = "TACTICAL GUIDE",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00E5FF),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    InstructionRow("STEER", "Hold Left / Right for analog lane shifts with momentum & lean.", Color(0xFF00E5FF))
                    Spacer(modifier = Modifier.height(8.dp))
                    InstructionRow("CYBER DASH", "Double-tap Left / Right for instant invincible thruster roll!", Color(0xFF00E5FF))
                    Spacer(modifier = Modifier.height(8.dp))
                    InstructionRow("JUMP", "Tuck & leap over straight boulders and bouncers.", Color(0xFFFFD600))
                    Spacer(modifier = Modifier.height(8.dp))
                    InstructionRow("OVERDRIVE", "Collect energy & tap OVERDRIVE to smash boulders at 3X score!", Color(0xFFFFD54F))
                    Spacer(modifier = Modifier.height(8.dp))
                    InstructionRow("STYLE RANK", "Chain dodges & near-misses for S & SSS rank score multipliers!", Color(0xFFFF1744))
                    Spacer(modifier = Modifier.height(8.dp))
                    InstructionRow("BRAKE", "Decelerate forward speed to let diagonal crossers roll by.", Color(0xFFFF5252))
                    Spacer(modifier = Modifier.height(8.dp))
                    InstructionRow("SHIELD", "Absorbs 1 direct boulder hit so you can continue running!", Color(0xFF00E5FF))
                    Spacer(modifier = Modifier.height(8.dp))
                    InstructionRow("CLOSE CALL", "Brush closely past boulders for +200 pts & instant combo!", Color(0xFF80D8FF))

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            viewModel.playMenuClick()
                            showHowToPlay = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("LET'S ROLL!", color = Color(0xFF0A1020), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // HIGH SCORES & CAREER STATS MODAL
    if (showHighScores) {
        HighScoresDialog(
            viewModel = viewModel,
            onDismiss = {
                viewModel.playMenuBack()
                showHighScores = false
            }
        )
    }
}

@Composable
private fun RatingIndicator(
    label: String,
    rating: Int,
    maxRating: Int = 5,
    accent: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0x99FFFFFF),
            modifier = Modifier.width(36.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.5.dp)) {
            for (i in 1..maxRating) {
                Box(
                    modifier = Modifier
                        .size(width = 10.dp, height = 4.5.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(
                            if (i <= rating) accent else Color(0x28FFFFFF)
                        )
                )
            }
        }
    }
}

@Composable
fun HighScoresDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    val top10 by viewModel.top10Records.collectAsStateWithLifecycle()
    val career by viewModel.careerStats.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1528)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FFD54F)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Trophy
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = AppIcons.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HALL OF FAME",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD54F),
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs: Top Runs vs Career Stats
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0x18FFFFFF),
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFFFFD54F)
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("TOP RUNS", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("CAREER STATS", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    // Leaderboard List
                    if (top10.isEmpty()) {
                        Text(
                            text = "No runs recorded yet.\nSurvive boulders to build your legacy!",
                            fontSize = 13.sp,
                            color = Color(0x88FFFFFF),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 36.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(top10) { idx, item ->
                                val rankBadge = when (idx) {
                                    0 -> "🥇"
                                    1 -> "🥈"
                                    2 -> "🥉"
                                    else -> "#${idx + 1}"
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (idx == 0) Color(0x33FFD54F) else Color(0x1AFFFFFF),
                                    border = if (idx == 0) androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFFFD54F)) else null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = rankBadge,
                                                fontSize = if (idx < 3) 18.sp else 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (idx == 0) Color(0xFFFFD54F) else Color.White,
                                                modifier = Modifier.width(32.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "${item.score} pts",
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.White,
                                                    fontSize = 15.sp
                                                )
                                                Text(
                                                    text = "${item.distanceMeters}m • ${item.ballsDodged} dodged • ${item.maxCombo}x combo",
                                                    color = Color(0xAAFFFFFF),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = item.sectorReached,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00E5FF)
                                            )
                                            Text(
                                                text = item.difficultyMode.uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (item.difficultyMode == "Overdrive") Color(0xFFFF5722) else Color(0xFF78909C)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Career Lifetime Stats Tab
                    val runnerLevel = 1 + (career.totalScore / 5000).toInt()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x33FFD54F),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "RUNNER LICENSE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFD54F)
                                    )
                                    Text(
                                        text = "LEVEL $runnerLevel",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "${career.totalScore} Total XP",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFE082)
                                )
                            }
                        }

                        CareerStatRow("Total Distance Run", "${career.totalDistance} meters", Color(0xFF80D8FF))
                        CareerStatRow("Total Boulders Dodged", "${career.totalDodged}", Color(0xFF81C784))
                        CareerStatRow("Highest Combo Chain", "${career.maxComboEver}X", Color(0xFFFF80AB))
                        CareerStatRow("Total Games Played", "${career.totalGamesPlayed}", Color(0xFFB0BEC5))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("BACK TO GAME", color = Color(0xFF0A1020), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun CareerStatRow(title: String, value: String, accentColor: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0x18FFFFFF),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 12.sp, color = Color(0xFFB0BEC5))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accentColor)
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
            modifier = Modifier.width(96.dp)
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = accentColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = desc,
            fontSize = 11.sp,
            color = Color(0xDDFFFFFF),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun CharacterHangarDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    val profile by viewModel.playerProfile.collectAsStateWithLifecycle()
    val walletPoints = profile?.totalPoints ?: 0
    val equippedId = profile?.equippedCharacterId ?: "vanguard"
    val unlockedSet = (profile?.unlockedCharacterIds ?: "vanguard")
        .split(",").map { it.trim().lowercase() }.toSet()

    var selectedModel by remember {
        mutableStateOf(CharacterModelId.fromId(equippedId))
    }

    val isEquipped = equippedId.equals(selectedModel.id, ignoreCase = true)
    val isUnlocked = unlockedSet.contains(selectedModel.id.lowercase())
    val canAfford = walletPoints >= selectedModel.price

    val modelAccentColor = when (selectedModel) {
        CharacterModelId.VANGUARD -> Color(0xFF00E5FF)
        CharacterModelId.TITAN -> Color(0xFFFF7043)
        CharacterModelId.VALKYRIE -> Color(0xFF00E676)
        CharacterModelId.PHANTOM -> Color(0xFFE040FB)
        CharacterModelId.CHRONOS -> Color(0xFFFFD54F)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF210162B)),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, modelAccentColor.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("dialog_character_hangar")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // MODAL HEADER (Title, Wallet Points Chip, Close)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "3D PILOT HANGAR",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                        Text(
                            text = "CUSTOM 3D RUNNER MODELS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = Color(0xFF80D8FF)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0x3300E5FF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6600E5FF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AppIcons.Stars,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$walletPoints PTS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // CHARACTER SELECTION TABS ROW
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(CharacterModelId.entries) { model ->
                        val isSelected = selectedModel == model
                        val isOwned = unlockedSet.contains(model.id.lowercase())
                        val isCurrentlyEquipped = equippedId.equals(model.id, ignoreCase = true)
                        val color = when (model) {
                            CharacterModelId.VANGUARD -> Color(0xFF00E5FF)
                            CharacterModelId.TITAN -> Color(0xFFFF7043)
                            CharacterModelId.VALKYRIE -> Color(0xFF00E676)
                            CharacterModelId.PHANTOM -> Color(0xFFE040FB)
                            CharacterModelId.CHRONOS -> Color(0xFFFFD54F)
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) color.copy(alpha = 0.25f) else Color(0x18FFFFFF),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) color else Color(0x33FFFFFF)
                            ),
                            modifier = Modifier
                                .clickable { selectedModel = model }
                                .testTag("tab_hangar_${model.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = model.displayName.split(" ").first(),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xBBFFFFFF)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                if (isCurrentlyEquipped) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Equipped",
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(12.dp)
                                    )
                                } else if (!isOwned) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SELECTED CHARACTER SHOWCASE CARD
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0x280A1020),
                    border = androidx.compose.foundation.BorderStroke(1.dp, modelAccentColor.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        // Title & Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = selectedModel.displayName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = modelAccentColor
                                )
                                Text(
                                    text = selectedModel.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB0BEC5)
                                )
                            }

                            // Price or Status Badge
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    isEquipped -> Color(0x3300E676)
                                    isUnlocked -> Color(0x3300E5FF)
                                    else -> Color(0x33FFD54F)
                                }
                            ) {
                                Text(
                                    text = when {
                                        isEquipped -> "EQUIPPED"
                                        isUnlocked -> "OWNED"
                                        selectedModel.price == 0 -> "FREE"
                                        else -> "${selectedModel.price} PTS"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = when {
                                        isEquipped -> Color(0xFF00E676)
                                        isUnlocked -> Color(0xFF00E5FF)
                                        else -> Color(0xFFFFD54F)
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 3D Silhouette / Mesh Architecture Description
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x18FFFFFF),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "3D MESH ARCHITECTURE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = modelAccentColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = selectedModel.description,
                                    fontSize = 11.sp,
                                    color = Color(0xEEFFFFFF),
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // STAT BARS
                        StatMeter("Armor Plating", selectedModel.armorRating, 5, modelAccentColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        StatMeter("Sprint Velocity", selectedModel.speedRating, 5, modelAccentColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        StatMeter("Tech & Perks", selectedModel.techRating, 5, modelAccentColor)

                        Spacer(modifier = Modifier.height(10.dp))

                        // SIGNATURE PERK CARD
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = modelAccentColor.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, modelAccentColor.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = AppIcons.Security,
                                    contentDescription = null,
                                    tint = modelAccentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = selectedModel.perkName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = modelAccentColor
                                    )
                                    Text(
                                        text = selectedModel.perkDescription,
                                        fontSize = 10.sp,
                                        color = Color(0xDDFFFFFF)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ACTION BUTTON: Equip / Purchase
                when {
                    isEquipped -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x2200E676),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E676)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ACTIVE PILOT EQUIPPED",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF00E676),
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                    isUnlocked -> {
                        Button(
                            onClick = { viewModel.selectOrPurchaseCharacter(selectedModel) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = modelAccentColor,
                                contentColor = Color(0xFF0A1020)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_equip_${selectedModel.id}")
                        ) {
                            Text(
                                text = "DEPLOY ${selectedModel.displayName.uppercase()}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    canAfford -> {
                        Button(
                            onClick = { viewModel.selectOrPurchaseCharacter(selectedModel) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFD54F),
                                contentColor = Color(0xFF1A1202)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_purchase_${selectedModel.id}")
                        ) {
                            Icon(imageVector = AppIcons.Stars, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "UNLOCK & EQUIP • ${selectedModel.price} PTS",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    else -> {
                        Button(
                            onClick = { },
                            enabled = false,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LOCKED (${selectedModel.price} PTS • NEED ${selectedModel.price - walletPoints} MORE)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // BONUS TEST GRANT BUTTON & CLOSE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "+1,000 PTS (Practice Bonus)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F),
                        modifier = Modifier
                            .clickable { viewModel.addBonusPoints(1000) }
                            .padding(4.dp)
                            .testTag("btn_add_practice_points")
                    )

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_close_hangar")
                    ) {
                        Text("CLOSE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMeter(
    label: String,
    value: Int,
    max: Int,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFFB0BEC5),
            modifier = Modifier.width(95.dp)
        )
        LinearProgressIndicator(
            progress = { (value.toFloat() / max.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = accentColor,
            trackColor = Color(0x33FFFFFF)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$value/$max",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

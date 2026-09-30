package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * TouchController component in Compose that provides callbacks for LEFT, RIGHT, JUMP,
 * and BRAKE inputs to ensure smooth analog movement and control for the player character.
 *
 * Supports multi-touch input tracking (Touch Down, Touch Held, Touch Released),
 * visual spring feedback, and accessibility compliance.
 */
@Composable
fun TouchController(
    onLeftChange: (Boolean) -> Unit,
    onRightChange: (Boolean) -> Unit,
    onJump: () -> Unit,
    onShootChange: (Boolean) -> Unit = {},
    onShoot: () -> Unit = {},
    onBrakeChange: (Boolean) -> Unit = {},
    onDashLeft: () -> Unit = {},
    onDashRight: () -> Unit = {},
    onOverdrive: () -> Unit = {},
    overdriveEnergy: Float = 0f,
    isOverdriveActive: Boolean = false,
    ammo: Int = 10,
    isLandscape: Boolean = false,
    modifier: Modifier = Modifier
) {
    val hasAmmo = ammo > 0

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
    ) {
        if (isLandscape) {
            // Landscape Mode:
            // LEFT is at the bottom-left corner, JUMP is inner.
            // SHOOT is inner (swapped away from corner), RIGHT is at the bottom-right corner.
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("touch_controller")
            ) {
                // BOTTOM LEFT CORNER: LEFT + JUMP
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TouchPadButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        label = "LEFT",
                        testTag = "btn_steer_left",
                        size = 64.dp,
                        activeColor = Color(0xFF00E5FF),
                        onHoldChange = onLeftChange,
                        onDoubleTap = onDashLeft
                    )
                    TouchPadButton(
                        icon = AppIcons.ArrowUpward,
                        label = "JUMP",
                        testTag = "btn_jump",
                        size = 64.dp,
                        activeColor = Color(0xFF00E5FF),
                        onHoldChange = {},
                        onPressDown = onJump
                    )
                }

                // CENTER: CHRONO OVERDRIVE ULTIMATE
                OverdriveButton(
                    energy = overdriveEnergy,
                    isActive = isOverdriveActive,
                    onActivate = onOverdrive,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
                )

                // BOTTOM RIGHT: SHOOT (inner) + RIGHT (bottom-right corner)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TouchPadButton(
                        icon = AppIcons.FlashOn,
                        label = if (hasAmmo) "SHOOT" else "EMPTY",
                        testTag = "btn_shoot",
                        size = 64.dp,
                        activeColor = if (hasAmmo) Color(0xFFFF3D00) else Color(0xFFFF5252),
                        badge = "$ammo",
                        badgeColor = if (hasAmmo) Color(0xFFFF6D00) else Color(0xFFD50000),
                        onHoldChange = onShootChange,
                        onPressDown = onShoot
                    )
                    TouchPadButton(
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        label = "RIGHT",
                        testTag = "btn_steer_right",
                        size = 64.dp,
                        activeColor = Color(0xFF00E5FF),
                        onHoldChange = onRightChange,
                        onDoubleTap = onDashRight
                    )
                }
            }
        } else {
            // Portrait Mode:
            // 1. LEFT at the bottom-left corner + JUMP (inner)
            // 2. SHOOT (inner, not at corner) + RIGHT at the bottom-right corner
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("touch_controller")
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left group: LEFT (at bottom-left corner) + JUMP
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TouchPadButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        label = "LEFT",
                        testTag = "btn_steer_left",
                        size = 60.dp,
                        activeColor = Color(0xFF00E5FF),
                        onHoldChange = onLeftChange,
                        onDoubleTap = onDashLeft
                    )
                    TouchPadButton(
                        icon = AppIcons.ArrowUpward,
                        label = "JUMP",
                        testTag = "btn_jump",
                        size = 60.dp,
                        activeColor = Color(0xFF00E5FF),
                        onHoldChange = {},
                        onPressDown = onJump
                    )
                }

                // Center: Chrono Overdrive Ultimate
                OverdriveButton(
                    energy = overdriveEnergy,
                    isActive = isOverdriveActive,
                    onActivate = onOverdrive,
                    size = 54.dp
                )

                // Right group: SHOOT (inner, not at corner) + RIGHT (at corner)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TouchPadButton(
                        icon = AppIcons.FlashOn,
                        label = if (hasAmmo) "SHOOT" else "EMPTY",
                        testTag = "btn_shoot",
                        size = 60.dp,
                        activeColor = if (hasAmmo) Color(0xFFFF3D00) else Color(0xFFFF5252),
                        badge = "$ammo",
                        badgeColor = if (hasAmmo) Color(0xFFFF6D00) else Color(0xFFD50000),
                        onHoldChange = onShootChange,
                        onPressDown = onShoot
                    )
                    TouchPadButton(
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        label = "RIGHT",
                        testTag = "btn_steer_right",
                        size = 60.dp,
                        activeColor = Color(0xFF00E5FF),
                        onHoldChange = onRightChange,
                        onDoubleTap = onDashRight
                    )
                }
            }
        }
    }
}

/**
 * Low-latency touch button supporting continuous touch tracking (Down, Held, Up)
 * with spring scale and glowing border feedback.
 */
@Composable
fun TouchPadButton(
    icon: ImageVector,
    label: String,
    testTag: String,
    activeColor: Color,
    onHoldChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onPressDown: (() -> Unit)? = null,
    onDoubleTap: (() -> Unit)? = null,
    size: Dp = 64.dp,
    badge: String? = null,
    badgeColor: Color = Color(0xFFFF6D00)
) {
    var isPressed by remember { mutableStateOf(false) }
    var lastTapTime by remember { mutableStateOf(0L) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = 0.55f),
        label = "touchBtnScale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isPressed) activeColor else Color(0x6600E5FF),
        label = "touchBtnBorder"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isPressed) activeColor.copy(alpha = 0.35f) else Color(0x73091325),
        label = "touchBtnBg"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(bgColor)
            .border(2.dp, borderColor, CircleShape)
            .testTag(testTag)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    val now = System.currentTimeMillis()
                    if (now - lastTapTime < 320L) {
                        onDoubleTap?.invoke()
                    }
                    lastTapTime = now

                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    onHoldChange(true)
                    onPressDown?.invoke()

                    var hasSwiped = false
                    do {
                        val event = awaitPointerEvent()
                        val currentPointer = event.changes.firstOrNull { it.id == down.id }
                        if (currentPointer != null && !hasSwiped && onDoubleTap != null) {
                            val dragDistance = currentPointer.position.x - down.position.x
                            if (kotlin.math.abs(dragDistance) > 36f) {
                                hasSwiped = true
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onDoubleTap.invoke()
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    isPressed = false
                    onHoldChange(false)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isPressed) activeColor else Color.White,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                color = if (isPressed) activeColor else Color(0xDDFFFFFF)
            )
            if (onDoubleTap != null) {
                Text(
                    text = "DASH",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xAA00E5FF),
                    letterSpacing = 0.5.sp
                )
            }
        }

        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 6.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = badge,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun OverdriveButton(
    energy: Float,
    isActive: Boolean,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    val isReady = energy >= 0.99f
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val infiniteTransition = rememberInfiniteTransition(label = "odPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "odScale"
    )

    val buttonScale = if (isActive || isReady) pulseScale else 1.0f

    val borderColor = when {
        isActive -> Color(0xFFFFD54F)
        isReady -> Color(0xFFFFB300)
        else -> Color(0x5500E5FF)
    }

    val bgColor = when {
        isActive -> Color(0x99FF8F00)
        isReady -> Color(0x88FF6F00)
        else -> Color(0x66060F1E)
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(buttonScale)
            .clip(CircleShape)
            .background(bgColor)
            .border(2.dp, borderColor, CircleShape)
            .testTag("btn_overdrive")
            .pointerInput(isReady || isActive) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    if (isReady && !isActive) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onActivate()
                    }
                    waitForUpOrCancellation()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = AppIcons.Bolt,
                contentDescription = "Chrono Overdrive",
                tint = if (isReady || isActive) Color(0xFFFFE082) else Color(0x8800E5FF),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = when {
                    isActive -> "SURGE"
                    isReady -> "OVERDRIVE"
                    else -> "${(energy * 100).toInt()}%"
                },
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                color = if (isReady || isActive) Color.White else Color(0xAA00E5FF)
            )
            if (isReady && !isActive) {
                Text(
                    text = "READY!",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD54F)
                )
            }
        }
    }
}

package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
    ammo: Int = 10,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("touch_controller"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. LEFT Button
        TouchPadButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            label = "LEFT",
            testTag = "btn_steer_left",
            size = 64.dp,
            activeColor = Color(0xFF00E5FF),
            onHoldChange = onLeftChange
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(12.dp))

        // 2. SHOOT Button with live Ammo counter & Empty indicator
        val hasAmmo = ammo > 0
        TouchPadButton(
            icon = Icons.Default.FlashOn,
            label = if (hasAmmo) "SHOOT" else "EMPTY",
            testTag = "btn_shoot",
            size = 64.dp,
            activeColor = if (hasAmmo) Color(0xFFFF3D00) else Color(0xFFFF5252),
            badge = "$ammo",
            badgeColor = if (hasAmmo) Color(0xFFFF6D00) else Color(0xFFD50000),
            onHoldChange = onShootChange,
            onPressDown = onShoot
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(12.dp))

        // 3. JUMP Button
        TouchPadButton(
            icon = Icons.Default.ArrowUpward,
            label = "JUMP",
            testTag = "btn_jump",
            size = 64.dp,
            activeColor = Color(0xFF00E5FF),
            onHoldChange = {},
            onPressDown = onJump
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(12.dp))

        // 4. RIGHT Button
        TouchPadButton(
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            label = "RIGHT",
            testTag = "btn_steer_right",
            size = 64.dp,
            activeColor = Color(0xFF00E5FF),
            onHoldChange = onRightChange
        )
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
    size: Dp = 64.dp,
    badge: String? = null,
    badgeColor: Color = Color(0xFFFF6D00)
) {
    var isPressed by remember { mutableStateOf(false) }

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
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    onHoldChange(true)
                    onPressDown?.invoke()

                    waitForUpOrCancellation()
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

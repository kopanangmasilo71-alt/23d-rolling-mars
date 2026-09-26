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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
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
    onBrakeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("touch_controller"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        // FAR LEFT: Steer Left Button
        TouchPadButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            label = "LEFT",
            testTag = "btn_steer_left",
            size = 72.dp,
            activeColor = Color(0xFF00E5FF),
            onHoldChange = onLeftChange
        )

        // CENTER: Action Buttons (BRAKE & JUMP)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TouchPadButton(
                icon = Icons.Default.Speed,
                label = "BRAKE",
                testTag = "btn_brake",
                size = 66.dp,
                activeColor = Color(0xFFFF5252),
                onHoldChange = onBrakeChange
            )

            TouchPadButton(
                icon = Icons.Default.ArrowUpward,
                label = "JUMP",
                testTag = "btn_jump",
                size = 72.dp,
                activeColor = Color(0xFFFFD600),
                onHoldChange = {},
                onPressDown = onJump
            )
        }

        // FAR RIGHT: Steer Right Button
        TouchPadButton(
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            label = "RIGHT",
            testTag = "btn_steer_right",
            size = 72.dp,
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
    size: Dp = 70.dp
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = 0.55f),
        label = "touchBtnScale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isPressed) activeColor else Color(0x55FFFFFF),
        label = "touchBtnBorder"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isPressed) activeColor.copy(alpha = 0.35f) else Color(0x88121728),
        label = "touchBtnBg"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(bgColor)
            .border(2.5.dp, borderColor, CircleShape)
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
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPressed) activeColor else Color(0xCCFFFFFF)
            )
        }
    }
}

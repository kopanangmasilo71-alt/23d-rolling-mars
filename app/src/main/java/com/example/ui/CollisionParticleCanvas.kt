package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 2D Screen-space particle representing an expanding, billowing volumetric dust cloud puff.
 */
class DustPuffParticle(
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var radius: Float = 10f,
    var maxRadius: Float = 45f,
    var alpha: Float = 0.7f,
    var initialAlpha: Float = 0.7f,
    var life: Float = 0f,
    var maxLife: Float = 0.8f,
    var color: Color = Color(0xFFA1887F)
) {
    val isAlive: Boolean get() = life < maxLife

    fun update(dt: Float) {
        if (!isAlive) return
        life += dt
        val progress = (life / maxLife).coerceIn(0f, 1f)

        x += vx * dt
        y += vy * dt

        // Drag decelerates dust puff
        vx *= 0.93f
        vy *= 0.93f

        // Natural expansion
        radius = radius + (maxRadius - radius) * (dt * 3.5f)

        // Smooth cubic fade-out
        val fadeProgress = 1f - progress
        alpha = initialAlpha * (fadeProgress * fadeProgress)
    }
}

/**
 * 2D Screen-space particle representing high-energy, incandescent collision sparks.
 */
class ImpactSparkParticle(
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var width: Float = 2.5f,
    var tailLength: Float = 0.04f,
    var alpha: Float = 1.0f,
    var life: Float = 0f,
    var maxLife: Float = 0.45f,
    var gravity: Float = 1200f,
    var coreColor: Color = Color.White,
    var glowColor: Color = Color(0xFFFFB300)
) {
    val isAlive: Boolean get() = life < maxLife

    fun update(dt: Float) {
        if (!isAlive) return
        life += dt
        val progress = (life / maxLife).coerceIn(0f, 1f)

        x += vx * dt
        y += vy * dt

        // Downward gravity arc
        vy += gravity * dt

        // Atmospheric drag
        vx *= 0.96f
        vy *= 0.98f

        // Fade out toward end of trajectory
        val fadeProgress = 1f - progress
        alpha = (fadeProgress * fadeProgress).coerceIn(0f, 1f)
    }
}

/**
 * 2D Screen-space expanding shockwave ripple.
 */
class ShockwaveRing(
    var x: Float = 0f,
    var y: Float = 0f,
    var radius: Float = 8f,
    var maxRadius: Float = 120f,
    var strokeWidth: Float = 6f,
    var alpha: Float = 0.85f,
    var life: Float = 0f,
    var maxLife: Float = 0.35f,
    var color: Color = Color(0xFFFFD54F)
) {
    val isAlive: Boolean get() = life < maxLife

    fun update(dt: Float) {
        if (!isAlive) return
        life += dt
        val progress = (life / maxLife).coerceIn(0f, 1f)

        radius += (maxRadius - radius) * (dt * 8.0f)
        strokeWidth = (6f * (1f - progress)).coerceAtLeast(1f)
        alpha = (1f - progress) * 0.85f
    }
}

/**
 * High-performance, zero-allocation particle system rendered with Canvas in Compose.
 * Creates billowing volumetric dust clouds, incandescent impact sparks, and shockwaves
 * when the player collides with boulders or unleashes defensive/offensive blasts.
 */
@Composable
fun CollisionParticleCanvas(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val impactEvent by viewModel.collisionImpactEvent.collectAsStateWithLifecycle()

    val dustParticles = remember { mutableStateListOf<DustPuffParticle>() }
    val sparkParticles = remember { mutableStateListOf<ImpactSparkParticle>() }
    val shockwaves = remember { mutableStateListOf<ShockwaveRing>() }

    var hasActiveParticles by remember { mutableStateOf(false) }

    val density = androidx.compose.ui.platform.LocalDensity.current

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        // Spawn particle bursts when a collision impact occurs
        LaunchedEffect(impactEvent?.id) {
            val event = impactEvent ?: return@LaunchedEffect

            val spawnX = if (event.screenX > 0f) event.screenX else (event.normalizedX * widthPx)
            val spawnY = if (event.screenY > 0f) event.screenY else (event.normalizedY * heightPx)

            // Clean up completed particles
            dustParticles.removeAll { !it.isAlive }
            sparkParticles.removeAll { !it.isAlive }
            shockwaves.removeAll { !it.isAlive }

            // Color palettes based on impact type
            val dustColor: Color
            val sparkCore: Color
            val sparkGlow: Color
            val ringColor: Color
            val sparkCount: Int
            val dustCount: Int

            when (event.impactType) {
                ImpactType.FATAL_BOULDER_CRASH -> {
                    // Massive boulder collision: heavy concrete/rock dust + fiery molten orange sparks
                    dustColor = Color(0xFF8D6E63)
                    sparkCore = Color(0xFFFFFFFF)
                    sparkGlow = Color(0xFFFF3D00)
                    ringColor = Color(0xFFFF5252)
                    sparkCount = 48
                    dustCount = 28
                }
                ImpactType.SHIELD_DEFLECT -> {
                    // Energy shield deflection: electric cyan plasma dust + radiant cyan/white sparks
                    dustColor = Color(0xFF80DEEA)
                    sparkCore = Color(0xFFE0F7FA)
                    sparkGlow = Color(0xFF00E5FF)
                    ringColor = Color(0xFF00E5FF)
                    sparkCount = 40
                    dustCount = 22
                }
                ImpactType.OVERDRIVE_CRUSH -> {
                    // Chrono Overdrive supersonic obliteration: incandescent gold dust + electric arcs
                    dustColor = Color(0xFFFFE082)
                    sparkCore = Color(0xFFFFFFFF)
                    sparkGlow = Color(0xFFFFD54F)
                    ringColor = Color(0xFFFFC107)
                    sparkCount = 55
                    dustCount = 32
                }
                ImpactType.BOULDER_BLAST -> {
                    // Blaster laser detonation: bright plasma sparks + rock dust puff
                    dustColor = Color(0xFFB0BEC5)
                    sparkCore = Color(0xFFFFFFFF)
                    sparkGlow = Color(0xFFFF6D00)
                    ringColor = Color(0xFFFF9800)
                    sparkCount = 36
                    dustCount = 20
                }
            }

            // Spawn dust puff clouds
            for (i in 0 until dustCount) {
                val angle = Random.nextFloat() * 2f * PI.toFloat()
                val speed = Random.nextFloat() * 220f + 40f
                val baseRadius = Random.nextFloat() * 12f + 8f
                val maxRadius = baseRadius + Random.nextFloat() * 32f + 16f
                val maxLife = Random.nextFloat() * 0.45f + 0.55f
                val initialAlpha = Random.nextFloat() * 0.35f + 0.40f

                dustParticles.add(
                    DustPuffParticle(
                        x = spawnX,
                        y = spawnY,
                        vx = cos(angle) * speed,
                        vy = sin(angle) * speed * 0.65f - 60f, // Upward buoyant billow
                        radius = baseRadius,
                        maxRadius = maxRadius,
                        alpha = initialAlpha,
                        initialAlpha = initialAlpha,
                        life = 0f,
                        maxLife = maxLife,
                        color = dustColor
                    )
                )
            }

            // Spawn incandescent kinetic sparks
            for (i in 0 until sparkCount) {
                val angle = Random.nextFloat() * 2f * PI.toFloat()
                val speed = Random.nextFloat() * 750f + 250f
                val maxLife = Random.nextFloat() * 0.25f + 0.28f
                val width = Random.nextFloat() * 2.2f + 1.8f
                val tailLength = Random.nextFloat() * 0.035f + 0.025f

                sparkParticles.add(
                    ImpactSparkParticle(
                        x = spawnX,
                        y = spawnY,
                        vx = cos(angle) * speed,
                        vy = sin(angle) * speed - 120f, // Slight upward kinetic kick
                        width = width,
                        tailLength = tailLength,
                        alpha = 1.0f,
                        life = 0f,
                        maxLife = maxLife,
                        gravity = Random.nextFloat() * 400f + 1100f,
                        coreColor = sparkCore,
                        glowColor = sparkGlow
                    )
                )
            }

            // Spawn shockwave ring
            shockwaves.add(
                ShockwaveRing(
                    x = spawnX,
                    y = spawnY,
                    radius = 10f,
                    maxRadius = if (event.impactType == ImpactType.OVERDRIVE_CRUSH) 140f else 95f,
                    strokeWidth = 7f,
                    alpha = 0.85f,
                    life = 0f,
                    maxLife = 0.32f,
                    color = ringColor
                )
            )

            hasActiveParticles = true
        }

    // Animation frame loop driven by withFrameNanos (zero CPU overhead when idle)
    LaunchedEffect(hasActiveParticles) {
        if (!hasActiveParticles) return@LaunchedEffect

        var lastTime = 0L
        while (hasActiveParticles) {
            withFrameNanos { now ->
                if (lastTime == 0L) {
                    lastTime = now
                    return@withFrameNanos
                }
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTime = now

                var aliveCount = 0

                // Update dust
                for (p in dustParticles) {
                    if (p.isAlive) {
                        p.update(dt)
                        aliveCount++
                    }
                }

                // Update sparks
                for (s in sparkParticles) {
                    if (s.isAlive) {
                        s.update(dt)
                        aliveCount++
                    }
                }

                // Update shockwaves
                for (sw in shockwaves) {
                    if (sw.isAlive) {
                        sw.update(dt)
                        aliveCount++
                    }
                }

                if (aliveCount == 0) {
                    hasActiveParticles = false
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("collision_particle_canvas")
    ) {
        // 1. Draw billowing dust cloud puffs
        for (dp in dustParticles) {
            if (dp.isAlive && dp.alpha > 0.01f) {
                drawCircle(
                    color = dp.color.copy(alpha = dp.alpha),
                    radius = dp.radius,
                    center = Offset(dp.x, dp.y)
                )
            }
        }

        // 2. Draw shockwave expansion rings
        for (sw in shockwaves) {
            if (sw.isAlive && sw.alpha > 0.01f) {
                drawCircle(
                    color = sw.color.copy(alpha = sw.alpha),
                    radius = sw.radius,
                    center = Offset(sw.x, sw.y),
                    style = Stroke(width = sw.strokeWidth)
                )
            }
        }

        // 3. Draw incandescent impact sparks (glow halo + brilliant core streak)
        for (sp in sparkParticles) {
            if (sp.isAlive && sp.alpha > 0.01f) {
                val tailX = sp.x - sp.vx * sp.tailLength
                val tailY = sp.y - sp.vy * sp.tailLength

                // Outer radiant glow line
                drawLine(
                    color = sp.glowColor.copy(alpha = sp.alpha * 0.85f),
                    start = Offset(tailX, tailY),
                    end = Offset(sp.x, sp.y),
                    strokeWidth = sp.width * 2.2f,
                    cap = StrokeCap.Round
                )

                // Inner brilliant core line
                drawLine(
                    color = sp.coreColor.copy(alpha = sp.alpha),
                    start = Offset(tailX * 0.5f + sp.x * 0.5f, tailY * 0.5f + sp.y * 0.5f),
                    end = Offset(sp.x, sp.y),
                    strokeWidth = sp.width,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
}

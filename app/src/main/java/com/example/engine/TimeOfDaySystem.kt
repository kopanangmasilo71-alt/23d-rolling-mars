package com.example.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class TimeOfDayPhase(
    val title: String,
    val emoji: String,
    val clockStart: Float, // 24-hr decimal
    val description: String,
    val bannerText: String
) {
    DAWN("DAWN", "🌅", 5.5f, "Sunrise Horizon", "🌅 DAWN BREAKS"),
    MIDDAY("MIDDAY", "☀️", 11.0f, "High Noon Sunlight", "☀️ HIGH NOON"),
    SUNSET("SUNSET", "🌇", 17.5f, "Golden Hour Horizon", "🌇 GOLDEN HOUR"),
    DUSK("TWILIGHT", "🌆", 20.0f, "Evening Twilight", "🌆 TWILIGHT SETS"),
    NIGHT("NIGHT", "🌙", 22.5f, "Midnight Starfield", "🌙 MIDNIGHT RUN")
}

data class TimeOfDaySnapshot(
    val phase: TimeOfDayPhase,
    val phaseProgress: Float, // 0.0 .. 1.0 within current phase
    val cycleProgress: Float, // 0.0 .. 1.0 within entire day cycle
    val dayNumber: Int,
    val timeString: String,
    val lightDir: FloatArray,
    val lightColor: FloatArray,
    val ambientColor: FloatArray,
    val fogSkyColor: FloatArray,
    val fogHorizonColor: FloatArray,
    val sunPos: Vector3,
    val sunColor: FloatArray,
    val sunAlpha: Float,
    val moonPos: Vector3,
    val moonColor: FloatArray,
    val moonAlpha: Float,
    val starsAlpha: Float,
    val headlightIntensity: Float,
    val streetLightEmissive: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TimeOfDaySnapshot
        return phase == other.phase && dayNumber == other.dayNumber && timeString == other.timeString
    }

    override fun hashCode(): Int {
        var result = phase.hashCode()
        result = 31 * result + dayNumber
        result = 31 * result + timeString.hashCode()
        return result
    }
}

class TimeOfDaySystem(
    val cycleScoreLength: Int = 10000
) {
    private class Keyframe(
        val progress: Float,
        val phase: TimeOfDayPhase,
        val lightDir: FloatArray,
        val lightColor: FloatArray,
        val ambientColor: FloatArray,
        val fogSkyColor: FloatArray,
        val fogHorizonColor: FloatArray,
        val sunPos: Vector3,
        val sunColor: FloatArray,
        val sunAlpha: Float,
        val moonPos: Vector3,
        val moonColor: FloatArray,
        val moonAlpha: Float,
        val starsAlpha: Float,
        val headlightIntensity: Float,
        val streetLightEmissive: Float
    )

    // Keyframes spanning 0.0 to 1.0 with seamless wraparound
    private val keyframes = listOf(
        // 0.00: Early Dawn
        Keyframe(
            progress = 0.00f,
            phase = TimeOfDayPhase.DAWN,
            lightDir = normalize3(floatArrayOf(0.70f, 0.35f, 0.45f)),
            lightColor = floatArrayOf(1.00f, 0.72f, 0.52f),
            ambientColor = floatArrayOf(0.36f, 0.30f, 0.44f),
            fogSkyColor = floatArrayOf(0.12f, 0.09f, 0.24f),
            fogHorizonColor = floatArrayOf(0.96f, 0.45f, 0.22f),
            sunPos = Vector3(36f, 12f, -85f),
            sunColor = floatArrayOf(1.00f, 0.65f, 0.30f),
            sunAlpha = 0.85f,
            moonPos = Vector3(-45f, 4f, -85f),
            moonColor = floatArrayOf(0.75f, 0.85f, 1.0f),
            moonAlpha = 0.0f,
            starsAlpha = 0.30f,
            headlightIntensity = 0.15f,
            streetLightEmissive = 0.40f
        ),
        // 0.15: Golden Morning
        Keyframe(
            progress = 0.15f,
            phase = TimeOfDayPhase.DAWN,
            lightDir = normalize3(floatArrayOf(0.50f, 0.65f, 0.35f)),
            lightColor = floatArrayOf(1.00f, 0.90f, 0.75f),
            ambientColor = floatArrayOf(0.42f, 0.44f, 0.52f),
            fogSkyColor = floatArrayOf(0.10f, 0.20f, 0.42f),
            fogHorizonColor = floatArrayOf(0.92f, 0.68f, 0.42f),
            sunPos = Vector3(24f, 26f, -85f),
            sunColor = floatArrayOf(1.00f, 0.88f, 0.55f),
            sunAlpha = 1.0f,
            moonPos = Vector3(-60f, -10f, -85f),
            moonColor = floatArrayOf(0.75f, 0.85f, 1.0f),
            moonAlpha = 0.0f,
            starsAlpha = 0.0f,
            headlightIntensity = 0.0f,
            streetLightEmissive = 0.10f
        ),
        // 0.35: High Noon / Midday
        Keyframe(
            progress = 0.35f,
            phase = TimeOfDayPhase.MIDDAY,
            lightDir = normalize3(floatArrayOf(0.15f, 0.95f, 0.20f)),
            lightColor = floatArrayOf(1.00f, 0.98f, 0.92f),
            ambientColor = floatArrayOf(0.48f, 0.52f, 0.60f),
            fogSkyColor = floatArrayOf(0.06f, 0.22f, 0.52f),
            fogHorizonColor = floatArrayOf(0.68f, 0.84f, 1.00f),
            sunPos = Vector3(0f, 38f, -85f),
            sunColor = floatArrayOf(1.00f, 0.98f, 0.85f),
            sunAlpha = 1.0f,
            moonPos = Vector3(0f, -40f, -85f),
            moonColor = floatArrayOf(0.75f, 0.85f, 1.0f),
            moonAlpha = 0.0f,
            starsAlpha = 0.0f,
            headlightIntensity = 0.0f,
            streetLightEmissive = 0.05f
        ),
        // 0.55: Sunset / Golden Hour
        Keyframe(
            progress = 0.55f,
            phase = TimeOfDayPhase.SUNSET,
            lightDir = normalize3(floatArrayOf(-0.65f, 0.36f, 0.45f)),
            lightColor = floatArrayOf(1.00f, 0.48f, 0.16f),
            ambientColor = floatArrayOf(0.44f, 0.28f, 0.38f),
            fogSkyColor = floatArrayOf(0.18f, 0.06f, 0.28f),
            fogHorizonColor = floatArrayOf(1.00f, 0.35f, 0.10f),
            sunPos = Vector3(-32f, 12f, -85f),
            sunColor = floatArrayOf(1.00f, 0.38f, 0.12f),
            sunAlpha = 1.0f,
            moonPos = Vector3(40f, 6f, -85f),
            moonColor = floatArrayOf(0.70f, 0.85f, 1.0f),
            moonAlpha = 0.25f,
            starsAlpha = 0.10f,
            headlightIntensity = 0.25f,
            streetLightEmissive = 0.50f
        ),
        // 0.72: Twilight / Dusk
        Keyframe(
            progress = 0.72f,
            phase = TimeOfDayPhase.DUSK,
            lightDir = normalize3(floatArrayOf(-0.45f, 0.18f, 0.55f)),
            lightColor = floatArrayOf(0.68f, 0.32f, 0.52f),
            ambientColor = floatArrayOf(0.28f, 0.22f, 0.40f),
            fogSkyColor = floatArrayOf(0.06f, 0.04f, 0.18f),
            fogHorizonColor = floatArrayOf(0.58f, 0.18f, 0.40f),
            sunPos = Vector3(-50f, 1f, -85f),
            sunColor = floatArrayOf(0.85f, 0.22f, 0.15f),
            sunAlpha = 0.20f,
            moonPos = Vector3(25f, 22f, -85f),
            moonColor = floatArrayOf(0.80f, 0.90f, 1.0f),
            moonAlpha = 0.80f,
            starsAlpha = 0.65f,
            headlightIntensity = 0.70f,
            streetLightEmissive = 0.85f
        ),
        // 0.85: Midnight Night
        Keyframe(
            progress = 0.85f,
            phase = TimeOfDayPhase.NIGHT,
            lightDir = normalize3(floatArrayOf(-0.25f, 0.82f, -0.25f)),
            lightColor = floatArrayOf(0.52f, 0.70f, 0.98f),
            ambientColor = floatArrayOf(0.18f, 0.20f, 0.34f),
            fogSkyColor = floatArrayOf(0.02f, 0.03f, 0.09f),
            fogHorizonColor = floatArrayOf(0.08f, 0.20f, 0.35f),
            sunPos = Vector3(0f, -40f, -85f),
            sunColor = floatArrayOf(0.2f, 0.1f, 0.1f),
            sunAlpha = 0.0f,
            moonPos = Vector3(-10f, 34f, -85f),
            moonColor = floatArrayOf(0.88f, 0.94f, 1.0f),
            moonAlpha = 1.0f,
            starsAlpha = 1.0f,
            headlightIntensity = 0.95f,
            streetLightEmissive = 1.0f
        ),
        // 0.95: Deep Space Night
        Keyframe(
            progress = 0.95f,
            phase = TimeOfDayPhase.NIGHT,
            lightDir = normalize3(floatArrayOf(-0.15f, 0.75f, -0.20f)),
            lightColor = floatArrayOf(0.55f, 0.72f, 0.98f),
            ambientColor = floatArrayOf(0.20f, 0.22f, 0.36f),
            fogSkyColor = floatArrayOf(0.03f, 0.04f, 0.11f),
            fogHorizonColor = floatArrayOf(0.12f, 0.22f, 0.38f),
            sunPos = Vector3(10f, -35f, -85f),
            sunColor = floatArrayOf(0.2f, 0.1f, 0.1f),
            sunAlpha = 0.0f,
            moonPos = Vector3(-25f, 30f, -85f),
            moonColor = floatArrayOf(0.88f, 0.94f, 1.0f),
            moonAlpha = 1.0f,
            starsAlpha = 1.0f,
            headlightIntensity = 0.95f,
            streetLightEmissive = 1.0f
        )
    )

    fun evaluate(score: Int): TimeOfDaySnapshot {
        val nonNegScore = if (score < 0) 0 else score
        val dayNumber = (nonNegScore / cycleScoreLength) + 1
        val scoreInCycle = nonNegScore % cycleScoreLength
        val cycleProgress = (scoreInCycle.toFloat() / cycleScoreLength).coerceIn(0f, 0.9999f)

        // Find bounding keyframes with wraparound
        var idxA = keyframes.size - 1
        var idxB = 0
        for (i in 0 until keyframes.size - 1) {
            if (cycleProgress >= keyframes[i].progress && cycleProgress < keyframes[i + 1].progress) {
                idxA = i
                idxB = i + 1
                break
            }
        }

        val kA = keyframes[idxA]
        val kB = keyframes[idxB]

        val span = if (kB.progress > kA.progress) {
            kB.progress - kA.progress
        } else {
            (1.0f - kA.progress) + kB.progress
        }

        val offset = if (cycleProgress >= kA.progress) {
            cycleProgress - kA.progress
        } else {
            (1.0f - kA.progress) + cycleProgress
        }

        val rawT = (offset / span).coerceIn(0f, 1f)
        // Cubic Hermite smoothstep for velvet continuous transitions
        val t = rawT * rawT * (3.0f - 2.0f * rawT)

        val lightDir = normalize3(lerp3(kA.lightDir, kB.lightDir, t))
        val lightColor = lerp3(kA.lightColor, kB.lightColor, t)
        val ambientColor = lerp3(kA.ambientColor, kB.ambientColor, t)
        val fogSkyColor = lerp3(kA.fogSkyColor, kB.fogSkyColor, t)
        val fogHorizonColor = lerp3(kA.fogHorizonColor, kB.fogHorizonColor, t)

        val sunPos = Vector3(
            lerp(kA.sunPos.x, kB.sunPos.x, t),
            lerp(kA.sunPos.y, kB.sunPos.y, t),
            lerp(kA.sunPos.z, kB.sunPos.z, t)
        )
        val sunColor = lerp3(kA.sunColor, kB.sunColor, t)
        val sunAlpha = lerp(kA.sunAlpha, kB.sunAlpha, t)

        val moonPos = Vector3(
            lerp(kA.moonPos.x, kB.moonPos.x, t),
            lerp(kA.moonPos.y, kB.moonPos.y, t),
            lerp(kA.moonPos.z, kB.moonPos.z, t)
        )
        val moonColor = lerp3(kA.moonColor, kB.moonColor, t)
        val moonAlpha = lerp(kA.moonAlpha, kB.moonAlpha, t)

        val starsAlpha = lerp(kA.starsAlpha, kB.starsAlpha, t)
        val headlightIntensity = lerp(kA.headlightIntensity, kB.headlightIntensity, t)
        val streetLightEmissive = lerp(kA.streetLightEmissive, kB.streetLightEmissive, t)

        val activePhase = when {
            cycleProgress < 0.25f -> TimeOfDayPhase.DAWN
            cycleProgress < 0.50f -> TimeOfDayPhase.MIDDAY
            cycleProgress < 0.68f -> TimeOfDayPhase.SUNSET
            cycleProgress < 0.82f -> TimeOfDayPhase.DUSK
            else -> TimeOfDayPhase.NIGHT
        }

        val phaseProgress = when (activePhase) {
            TimeOfDayPhase.DAWN -> cycleProgress / 0.25f
            TimeOfDayPhase.MIDDAY -> (cycleProgress - 0.25f) / 0.25f
            TimeOfDayPhase.SUNSET -> (cycleProgress - 0.50f) / 0.18f
            TimeOfDayPhase.DUSK -> (cycleProgress - 0.68f) / 0.14f
            TimeOfDayPhase.NIGHT -> (cycleProgress - 0.82f) / 0.18f
        }.coerceIn(0f, 1f)

        // Simulated Clock calculation: Dawn starts at ~05:45 AM, progressing across 24h
        val totalMinutes = ((5.75f + cycleProgress * 24f) % 24f) * 60f
        val hour24 = (totalMinutes / 60f).toInt() % 24
        val minute = (totalMinutes % 60f).toInt()
        val amPm = if (hour24 < 12) "AM" else "PM"
        val hour12 = when (hour24 % 12) {
            0 -> 12
            else -> hour24 % 12
        }
        val timeString = String.format("%02d:%02d %s", hour12, minute, amPm)

        return TimeOfDaySnapshot(
            phase = activePhase,
            phaseProgress = phaseProgress,
            cycleProgress = cycleProgress,
            dayNumber = dayNumber,
            timeString = timeString,
            lightDir = lightDir,
            lightColor = lightColor,
            ambientColor = ambientColor,
            fogSkyColor = fogSkyColor,
            fogHorizonColor = fogHorizonColor,
            sunPos = sunPos,
            sunColor = sunColor,
            sunAlpha = sunAlpha,
            moonPos = moonPos,
            moonColor = moonColor,
            moonAlpha = moonAlpha,
            starsAlpha = starsAlpha,
            headlightIntensity = headlightIntensity,
            streetLightEmissive = streetLightEmissive
        )
    }

    companion object {
        fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

        fun lerp3(a: FloatArray, b: FloatArray, t: Float): FloatArray {
            return floatArrayOf(
                lerp(a[0], b[0], t),
                lerp(a[1], b[1], t),
                lerp(a[2], b[2], t)
            )
        }

        fun normalize3(v: FloatArray): FloatArray {
            val len = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
            val inv = if (len > 0.0001f) 1.0f / len else 1.0f
            return floatArrayOf(v[0] * inv, v[1] * inv, v[2] * inv)
        }
    }
}

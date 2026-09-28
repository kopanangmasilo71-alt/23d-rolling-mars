package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * High-performance, zero-asset procedural audio engine using native Android AudioTrack.
 * Generates distinct retro-arcade audio cues for running, jumping, braking, dodging, and crashes.
 */
class GameAudio {
    var isEnabled: Boolean = true
    private val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
    private val scope = CoroutineScope(Dispatchers.Default + exceptionHandler)

    private val sampleRate = 22050

    private fun playPcm(samples: ShortArray) {
        if (!isEnabled) return
        scope.launch {
            var audioTrack: AudioTrack? = null
            try {
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (minBufferSize <= 0) return@launch
                val bufferSize = maxOf(minBufferSize, samples.size * 2)

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                if (audioTrack.state == AudioTrack.STATE_INITIALIZED) {
                    audioTrack.write(samples, 0, samples.size)
                    audioTrack.play()
                    val durationMs = (samples.size * 1000L) / sampleRate + 50L
                    kotlinx.coroutines.delay(durationMs)
                }
            } catch (t: Throwable) {
                // Ignore audio hardware transient failures
            } finally {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (_: Throwable) {}
            }
        }
    }

    fun playJump() {
        val durationMs = 200
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Upward frequency sweep 200 Hz -> 580 Hz
            val freq = 200.0 + 380.0 * (i.toDouble() / numSamples)
            val envelope = (1.0 - (i.toDouble() / numSamples))
            val sample = sin(2.0 * PI * freq * t) * envelope * 0.7
            samples[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playBrake() {
        val durationMs = 120
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val noise = (Random.nextDouble() * 2.0 - 1.0) * 0.4
            val tone = sin(2.0 * PI * 130.0 * t) * 0.5
            val envelope = exp(-i.toDouble() / (numSamples * 0.5))
            val sample = (noise + tone) * envelope * 0.5
            samples[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playDodgeWhoosh() {
        val durationMs = 160
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val noise = (Random.nextDouble() * 2.0 - 1.0)
            val envelope = sin(progress * PI)
            val sample = noise * envelope * 0.45
            samples[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playCrash() {
        val durationMs = 450
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val rumble = sin(2.0 * PI * (90.0 - 50.0 * progress) * t) * 0.6
            val noise = (Random.nextDouble() * 2.0 - 1.0) * 0.7
            val envelope = (1.0 - progress) * (1.0 - progress)
            val sample = (rumble + noise) * envelope * 0.8
            samples[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playHighScoreChime() {
        val durationMs = 450
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val freq1 = 587.33 // D5
            val freq2 = 880.00 // A5
            val freq3 = 1174.66 // D6
            val envelope = (1.0 - progress)
            val sample = (sin(2.0 * PI * freq1 * t) * 0.4 + sin(2.0 * PI * freq2 * t) * 0.35 + sin(2.0 * PI * freq3 * t) * 0.25) * envelope
            samples[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playCombo(multiplier: Int) {
        val durationMs = 180
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)
        // Ascending pitches based on multiplier: C5, E5, G5, B5, C6
        val baseFreq = when (multiplier.coerceIn(1, 5)) {
            1 -> 523.25
            2 -> 659.25
            3 -> 783.99
            4 -> 987.77
            else -> 1046.50
        }
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val envelope = (1.0 - progress) * 0.8
            val sample = sin(2.0 * PI * baseFreq * t) * envelope
            samples[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playNearMiss() {
        val durationMs = 150
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            // Hyper-speed whoosh with laser whistle
            val freq = 1200.0 - 700.0 * progress
            val envelope = sin(progress * PI)
            val sample = (sin(2.0 * PI * freq * t) * 0.6 + (Random.nextDouble() * 2.0 - 1.0) * 0.3) * envelope
            samples[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playLandingImpact() {
        val durationMs = 140
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val freq = 110.0 - 55.0 * progress
            val envelope = (1.0 - progress) * (1.0 - progress)
            val noise = (Random.nextDouble() * 2.0 - 1.0) * 0.25
            val sample = (sin(2.0 * PI * freq * t) * 0.75 + noise) * envelope * 0.9
            samples[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playOrbCollect(orbNumber: Int = 1) {
        val durationMs = 180
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)
        val baseFreq = when (orbNumber) {
            1 -> 659.25 // E5
            2 -> 880.00 // A5
            else -> 1174.66 // D6
        }
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val envelope = (1.0 - progress)
            val sample = (sin(2.0 * PI * baseFreq * t) * 0.6 + sin(2.0 * PI * baseFreq * 2.0 * t) * 0.3) * envelope
            samples[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playBoostSurge() {
        val durationMs = 450
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            // Rising warp whoosh 250Hz -> 1400Hz
            val freq = 250.0 + 1150.0 * (progress * progress)
            val envelope = sin(progress * PI)
            val noise = (Random.nextDouble() * 2.0 - 1.0) * 0.35
            val sample = (sin(2.0 * PI * freq * t) * 0.65 + noise) * envelope
            samples[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playLaserShot() {
        val durationMs = 120
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            // Rapid high-tech sci-fi plasma chirp from 2400 Hz down to 320 Hz
            val freq = 2400.0 * (1.0 - progress * 0.86)
            val envelope = (1.0 - progress) * (1.0 - progress)
            val sample = sin(2.0 * PI * freq * t) * envelope * 0.90
            samples[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playBoulderExplode() {
        val durationMs = 280
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            // Heavy resonant sub-bass explosion crunch
            val freq = 130.0 - 90.0 * progress
            val envelope = (1.0 - progress) * (1.0 - progress)
            val noise = (Random.nextDouble() * 2.0 - 1.0) * 0.65
            val subBass = sin(2.0 * PI * freq * t) * 0.70
            val sample = (subBass + noise) * envelope * 0.95
            samples[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playDryFire() {
        val durationMs = 60
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val freq = 820.0 - 520.0 * progress
            val envelope = (1.0 - progress) * (1.0 - progress)
            val noise = (Random.nextDouble() * 2.0 - 1.0) * 0.45
            val sample = (sin(2.0 * PI * freq * t) * 0.6 + noise) * envelope * 0.85
            samples[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playAmmoPickup() {
        val durationMs = 180
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            // Ascending dual mechanical reload chime
            val freq1 = 523.25 + 523.25 * progress
            val freq2 = 659.25 + 659.25 * progress
            val envelope = (1.0 - progress * 0.4)
            val sample = (sin(2.0 * PI * freq1 * t) * 0.5 + sin(2.0 * PI * freq2 * t) * 0.4) * envelope * 0.8
            samples[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playSectorAlert() {
        val durationMs = 400
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            // Dual chord fanfare
            val chord = sin(2.0 * PI * 440.0 * t) * 0.4 + sin(2.0 * PI * 554.37 * t) * 0.35 + sin(2.0 * PI * 659.25 * t) * 0.25
            val envelope = (1.0 - progress * 0.8)
            samples[i] = ((chord * envelope).coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playSpeedPad() {
        val durationMs = 220
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val freq = 300.0 + 900.0 * progress // Rising power chirp
            val envelope = sin(progress * PI)
            val sample = sin(2.0 * PI * freq * t) * envelope * 0.8
            samples[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playShieldPickup() {
        val durationMs = 280
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            // Rising harmonic barrier resonance
            val freq1 = 440.0 + 600.0 * progress
            val freq2 = 880.0 + 1200.0 * progress
            val envelope = sin(progress * PI)
            val sample = (sin(2.0 * PI * freq1 * t) * 0.5 + sin(2.0 * PI * freq2 * t) * 0.3) * envelope
            samples[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playShieldDeflect() {
        val durationMs = 380
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            // Heavy metallic deflect impact followed by resonant energy discharge
            val zap = sin(2.0 * PI * (1200.0 - 900.0 * progress) * t) * 0.5
            val boom = sin(2.0 * PI * (150.0 - 90.0 * progress) * t) * 0.6
            val noise = (Random.nextDouble() * 2.0 - 1.0) * 0.4
            val envelope = (1.0 - progress) * (1.0 - progress)
            val sample = (zap + boom + noise) * envelope * 0.85
            samples[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playPowerUpPickup() {
        val durationMs = 240
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            // Bright sparkling arpeggio
            val freq = 523.25 + 700.0 * progress + sin(progress * 16.0) * 150.0
            val envelope = sin(progress * PI)
            val sample = sin(2.0 * PI * freq * t) * envelope * 0.75
            samples[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playUnlockSuccess() {
        val durationMs = 450
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)
        // Ascending major chord fanfare (C5 -> E5 -> G5 -> C6) with golden resonance
        val freqs = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val noteIdx = (progress * 4.0).toInt().coerceIn(0, 3)
            val noteFreq = freqs[noteIdx]
            val envelope = sin((progress % 0.25) * 4.0 * PI) * (1.0 - progress * 0.4)
            val sample = sin(2.0 * PI * noteFreq * t) * envelope * 0.8
            samples[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }

    fun playCharacterEquip() {
        val durationMs = 180
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            // High-tech magnetic lock sound (dual click snap)
            val f = if (progress < 0.5) 440.0 else 880.0
            val env = exp(-progress * 18.0) * sin(progress * PI)
            val sample = sin(2.0 * PI * f * t) * env * 0.9
            samples[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }
}

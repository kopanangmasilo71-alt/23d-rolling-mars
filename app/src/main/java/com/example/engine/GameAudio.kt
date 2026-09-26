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
        val durationMs = 350
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val freq1 = 587.33 // D5
            val freq2 = 880.00 // A5
            val envelope = (1.0 - progress)
            val sample = (sin(2.0 * PI * freq1 * t) * 0.5 + sin(2.0 * PI * freq2 * t) * 0.4) * envelope
            samples[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        playPcm(samples)
    }
}

package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.example.R
import java.util.Collections

/**
 * Robust, low-latency SoundManager utilizing Android's MediaPlayer to deliver
 * cinematic, high-impact sound effects for boulder collisions, jumping,
 * and snappy, tactile menu interactions.
 */
class SoundManager(context: Context? = null) {

    private val appContext: Context? = context?.applicationContext ?: context

    var isEnabled: Boolean = true
    var masterVolume: Float = 1.0f

    // Diagnostics / state tracking for inspection and testing
    var soundPlayCount: Int = 0
        private set
    var lastPlayedSound: String? = null
        private set

    // Thread-safe set of active MediaPlayers to handle concurrent sounds and clean disposal
    private val activePlayers = Collections.synchronizedSet(mutableSetOf<MediaPlayer>())

    companion object {
        private const val TAG = "SoundManager"

        @Volatile
        private var instance: SoundManager? = null

        fun getInstance(context: Context? = null): SoundManager {
            return instance ?: synchronized(this) {
                instance ?: SoundManager(context).also { instance = it }
            }
        }
    }

    /**
     * Internal playback helper that creates and prepares a MediaPlayer with game sonification attributes.
     */
    private fun playSound(soundName: String, resId: Int, volumeScale: Float = 1.0f) {
        lastPlayedSound = soundName
        soundPlayCount++

        if (!isEnabled || masterVolume <= 0f) return
        val ctx = appContext ?: return

        try {
            val effectiveVolume = (masterVolume * volumeScale).coerceIn(0f, 1f)

            val mp = MediaPlayer.create(ctx, resId) ?: run {
                Log.w(TAG, "MediaPlayer.create returned null for sound: $soundName")
                return
            }

            try {
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            } catch (_: Throwable) {
                // Backward-compatibility fallback
            }

            mp.setVolume(effectiveVolume, effectiveVolume)
            activePlayers.add(mp)

            mp.setOnCompletionListener { player ->
                try {
                    player.release()
                } catch (_: Throwable) {}
                activePlayers.remove(player)
            }

            mp.setOnErrorListener { player, what, extra ->
                Log.w(TAG, "MediaPlayer error on $soundName: what=$what, extra=$extra")
                try {
                    player.release()
                } catch (_: Throwable) {}
                activePlayers.remove(player)
                true
            }

            mp.start()
        } catch (t: Throwable) {
            Log.w(TAG, "Unable to play sound effect $soundName via MediaPlayer: ${t.message}")
        }
    }

    /**
     * Heavy concussive impact sound when the runner or an obstacle strikes a rolling boulder.
     */
    fun playBoulderCollision(volumeScale: Float = 1.0f) {
        playSound("boulder_collision", R.raw.boulder_collision, volumeScale)
    }

    /**
     * Energetic, buoyant athletic takeoff sound when the runner leaps into the air.
     */
    fun playJump(volumeScale: Float = 1.0f) {
        playSound("jump", R.raw.jump, volumeScale)
    }

    /**
     * Crisp, tactile mechanical click for standard UI buttons, toggles, and switches.
     */
    fun playMenuClick(volumeScale: Float = 1.0f) {
        playSound("menu_click", R.raw.menu_click, volumeScale)
    }

    /**
     * Resonant, futuristic harmonic chime for confirming choices, launching runs, and purchasing upgrades.
     */
    fun playMenuSelect(volumeScale: Float = 1.0f) {
        playSound("menu_select", R.raw.menu_select, volumeScale)
    }

    /**
     * Subtle downward melodic chime for back navigation, closing overlays, or canceling dialogs.
     */
    fun playMenuBack(volumeScale: Float = 1.0f) {
        playSound("menu_back", R.raw.menu_back, volumeScale)
    }

    /**
     * Explosive concussive blast when oncoming boulders are shattered by blaster plasma or overdrive.
     */
    fun playBoulderExplode(volumeScale: Float = 1.0f) {
        playSound("boulder_explode", R.raw.boulder_explode, volumeScale)
    }

    /**
     * Releases all currently playing MediaPlayer instances to free hardware resources.
     */
    fun release() {
        synchronized(activePlayers) {
            for (player in activePlayers) {
                try {
                    if (player.isPlaying) {
                        player.stop()
                    }
                    player.release()
                } catch (_: Throwable) {}
            }
            activePlayers.clear()
        }
    }
}

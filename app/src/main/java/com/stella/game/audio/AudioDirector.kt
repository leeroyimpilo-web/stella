package com.stella.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AudioDirector(context: Context) {
    private val assets = context.applicationContext.assets
    private var voicePlayer: MediaPlayer? = null
    private var ambiencePlayer: MediaPlayer? = null
    private var sfxPlayer: MediaPlayer? = null
    private var currentAmbience: String? = null

    var masterVolume: Float = 1f
    var voiceVolume: Float = 1f
    var ambienceVolume: Float = .42f
    var sfxVolume: Float = .72f

    suspend fun playVoiceAndWait(sceneId: String, lineIndex: Int): Boolean =
        suspendCancellableCoroutine { continuation ->
            stopVoice()
            val path = "voice/chapter1/" + sceneId + "_" + lineIndex.toString().padStart(2, '0') + ".mp3"
            val player = buildPlayer(path, loop = false)
            if (player == null) {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }

            voicePlayer = player
            player.setVolume(masterVolume * voiceVolume, masterVolume * voiceVolume)
            player.setOnCompletionListener {
                if (voicePlayer === it) voicePlayer = null
                it.release()
                if (continuation.isActive) continuation.resume(true)
            }
            player.setOnErrorListener { mp, _, _ ->
                if (voicePlayer === mp) voicePlayer = null
                mp.release()
                if (continuation.isActive) continuation.resume(false)
                true
            }
            continuation.invokeOnCancellation {
                if (voicePlayer === player) voicePlayer = null
                runCatching { player.stop() }
                player.release()
            }
            player.start()
        }

    fun playAmbience(key: String) {
        val file = when {
            key.contains("cryo", true) -> "cryo_hum.wav"
            key.contains("bridge", true) || key.contains("vault", true) -> "bridge_low.wav"
            key.contains("signal", true) || key.contains("static", true) -> "radio_static.wav"
            else -> "corridor_creaks.wav"
        }
        if (currentAmbience == file && ambiencePlayer?.isPlaying == true) return
        ambiencePlayer?.release()
        ambiencePlayer = buildPlayer("audio/ambience/" + file, loop = true)?.also {
            currentAmbience = file
            it.setVolume(masterVolume * ambienceVolume, masterVolume * ambienceVolume)
            it.start()
        }
    }

    fun playSfx(name: String) {
        val file = when (name) {
            "pickup" -> "pickup.wav"
            "alert" -> "alert.wav"
            else -> "choice.wav"
        }
        sfxPlayer?.release()
        sfxPlayer = buildPlayer("audio/sfx/" + file, loop = false)?.also {
            it.setVolume(masterVolume * sfxVolume, masterVolume * sfxVolume)
            it.setOnCompletionListener { mp ->
                if (sfxPlayer === mp) sfxPlayer = null
                mp.release()
            }
            it.start()
        }
    }

    fun stopVoice() {
        voicePlayer?.let {
            runCatching { if (it.isPlaying) it.stop() }
            it.release()
        }
        voicePlayer = null
    }

    fun release() {
        stopVoice()
        ambiencePlayer?.release()
        ambiencePlayer = null
        sfxPlayer?.release()
        sfxPlayer = null
        currentAmbience = null
    }

    private fun buildPlayer(path: String, loop: Boolean): MediaPlayer? {
        return runCatching {
            val afd = assets.openFd(path)
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                isLooping = loop
                prepare()
            }
        }.getOrNull()
    }
}

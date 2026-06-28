package com.example.seedie.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import androidx.annotation.RawRes
import androidx.media3.common.AudioAttributes as MediaAudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.seedie.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WordAudioPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val streamPlayer: ExoPlayer = ExoPlayer.Builder(appContext).build()
    private var rawPlayer: MediaPlayer? = null
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    init {
        streamPlayer.setAudioAttributes(
            MediaAudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build(),
            true
        )
        streamPlayer.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlayingValue: Boolean) {
                    _isPlaying.value = isPlayingValue || rawPlayer?.isPlaying == true
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        if (rawPlayer?.isPlaying != true) {
                            _isPlaying.value = false
                        }
                        abandonAudioFocus()
                    }
                }
            }
        )
    }

    fun playRaw(@RawRes resId: Int, onPlaybackFailed: (() -> Unit)? = null) {
        if (resId == 0) {
            logDebug("playRaw skipped: resId=0")
            onPlaybackFailed?.invoke()
            return
        }
        stopRawPlayer()
        streamPlayer.stop()
        streamPlayer.clearMediaItems()

        val focusGranted = requestAudioFocus()
        logDebug("playRaw resId=$resId audioFocusGranted=$focusGranted")

        val player = MediaPlayer.create(appContext, resId)
        if (player == null) {
            logDebug("playRaw MediaPlayer.create returned null for resId=$resId")
            onPlaybackFailed?.invoke()
            return
        }

        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        player.setOnCompletionListener {
            _isPlaying.value = false
            releaseRawPlayer()
            abandonAudioFocus()
        }
        player.setOnErrorListener { _, what, extra ->
            logDebug("playRaw MediaPlayer error what=$what extra=$extra")
            releaseRawPlayer()
            abandonAudioFocus()
            onPlaybackFailed?.invoke()
            true
        }

        rawPlayer = player
        runCatching {
            player.start()
            _isPlaying.value = true
            logDebug("playRaw started resId=$resId")
        }.onFailure { error ->
            logDebug("playRaw start failed: ${error.message}")
            releaseRawPlayer()
            abandonAudioFocus()
            onPlaybackFailed?.invoke()
        }
    }

    fun playUrl(url: String) {
        if (url.isBlank()) return
        stop()
        requestAudioFocus()
        streamPlayer.setMediaItem(MediaItem.fromUri(url))
        streamPlayer.prepare()
        streamPlayer.play()
    }

    fun stop() {
        stopRawPlayer()
        streamPlayer.stop()
        streamPlayer.clearMediaItems()
        _isPlaying.value = false
        abandonAudioFocus()
    }

    fun release() {
        stop()
        streamPlayer.release()
    }

    private fun stopRawPlayer() {
        rawPlayer?.let { player ->
            runCatching {
                if (player.isPlaying) {
                    player.stop()
                }
            }
            releaseRawPlayer()
        }
    }

    private fun releaseRawPlayer() {
        rawPlayer?.release()
        rawPlayer = null
    }

    private fun requestAudioFocus(): Boolean {
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        rawPlayer?.pause()
                        streamPlayer.pause()
                        _isPlaying.value = false
                    }
                }
            }
            .build()
        audioFocusRequest = focusRequest
        val result = audioManager.requestAudioFocus(focusRequest)
        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        if (!hasAudioFocus) return
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        hasAudioFocus = false
        audioFocusRequest = null
    }

    private fun logDebug(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }

    companion object {
        private const val TAG = "SeedieAudio"
    }
}

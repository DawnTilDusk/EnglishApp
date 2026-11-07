package com.example.seedie.ui.screens.learning.listening

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.seedie.data.audio.WordAudioPlayer
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class ListeningPlaybackState(
    val playing: Boolean = false,
    val paused: Boolean = false,
    val message: String? = null
)

/** Remote audio resumes at its exact position; TTS resumes at its latest spoken range/sentence. */
internal class ListeningPlaybackController(context: Context, scope: CoroutineScope) {
    private val audio = WordAudioPlayer(context)
    private val main = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(ListeningPlaybackState())
    val state = _state.asStateFlow()
    private var tts: TextToSpeech? = null
    private var ready = false
    private var speechUnavailable = false
    private var disposed = false
    private var usingSpeech = false
    private var speechText = ""
    private var speechCursor = 0
    private var generation = 0
    private var lastUtterance = ""
    private var currentUrl: String? = null
    private var currentText: String = ""
    private val audioJob = scope.launch {
        audio.isPlaying.collect { playing ->
            if (!usingSpeech) _state.update { it.copy(playing = playing) }
        }
    }

    init {
        tts = TextToSpeech(context) { status ->
            main.post {
                if (!disposed) {
                    ready = status == TextToSpeech.SUCCESS
                    speechUnavailable = !ready
                    if (ready) {
                        tts?.language = Locale.US
                        if (usingSpeech && _state.value.playing) queueSpeech()
                    } else if (usingSpeech) {
                        _state.value = ListeningPlaybackState(message = "设备语音不可用，请稍后重试。")
                    }
                }
            }
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            private fun current(id: String?, action: (Int) -> Unit) {
                main.post {
                    val parts = id?.split(":").orEmpty()
                    if (!disposed && parts.size == 2 && parts[0].toIntOrNull() == generation) {
                        parts[1].toIntOrNull()?.let(action)
                    }
                }
            }
            override fun onStart(utteranceId: String?) = current(utteranceId) { speechCursor = it }
            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) =
                current(utteranceId) { speechCursor = it + start }
            override fun onDone(utteranceId: String?) = current(utteranceId) {
                if (utteranceId == lastUtterance) _state.update { it.copy(playing = false, paused = false) }
            }
            @Deprecated("Required by UtteranceProgressListener")
            override fun onError(utteranceId: String?) = current(utteranceId) {
                _state.value = ListeningPlaybackState(message = "语音播放失败，请重试。")
            }
        })
    }

    fun play(url: String?, text: String) {
        currentUrl = url
        currentText = text
        generation++
        tts?.stop()
        audio.stop()
        usingSpeech = false
        _state.value = ListeningPlaybackState(playing = true)
        if (!url.isNullOrBlank()) audio.playUrl(url) { startSpeech(text) } else startSpeech(text)
    }

    private fun startSpeech(text: String) {
        usingSpeech = true
        if (speechUnavailable || text.isBlank()) {
            _state.value = ListeningPlaybackState(message = "当前无法朗读这篇材料，请稍后重试。")
            return
        }
        speechText = text
        speechCursor = 0
        _state.value = ListeningPlaybackState(playing = true, message = "当前使用设备语音朗读。")
        if (ready) queueSpeech()
    }

    private fun queueSpeech() {
        generation++
        val remaining = speechText.substring(speechCursor.coerceIn(0, speechText.length))
        val parts = Regex("[^.!?]+[.!?]*\\s*").findAll(remaining).toList()
        val offset = speechCursor
        parts.forEachIndexed { index, part ->
            val id = "$generation:${offset + part.range.first}"
            if (index == parts.lastIndex) lastUtterance = id
            val result = tts?.speak(part.value, if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, id)
            if (result == TextToSpeech.ERROR) _state.update { it.copy(playing = false, message = "语音播放失败，请重试。") }
        }
    }

    fun pause() {
        if (!_state.value.playing) return
        generation++
        if (usingSpeech) tts?.stop() else audio.pause()
        _state.update { it.copy(playing = false, paused = true) }
    }

    fun resume() {
        if (!_state.value.paused) return
        _state.update { it.copy(playing = true, paused = false) }
        if (usingSpeech) { if (ready) queueSpeech() } else audio.resume()
    }

    fun stop() {
        generation++
        usingSpeech = false
        tts?.stop()
        audio.stop()
        _state.value = ListeningPlaybackState()
        currentUrl = null
        currentText = ""
    }

    fun snapshot(): com.example.seedie.domain.model.ListeningPlaybackSnapshot? {
        if (currentUrl == null && currentText.isBlank()) return null
        return com.example.seedie.domain.model.ListeningPlaybackSnapshot(
            currentUrl, currentText, audio.currentPositionMs(), speechCursor, usingSpeech
        )
    }

    fun restore(saved: com.example.seedie.domain.model.ListeningPlaybackSnapshot) {
        stop()
        currentUrl = saved.url
        currentText = saved.text
        usingSpeech = saved.usingSpeech || saved.url.isNullOrBlank()
        speechText = saved.text
        speechCursor = saved.speechCursor.coerceIn(0, speechText.length)
        if (!usingSpeech) saved.url?.let { audio.prepareUrlPaused(it, saved.positionMs) }
        _state.value = ListeningPlaybackState(paused = true, message = "已恢复播放位置，点击播放继续")
    }

    fun release() {
        disposed = true
        audioJob.cancel()
        stop()
        audio.release()
        tts?.shutdown()
    }
}

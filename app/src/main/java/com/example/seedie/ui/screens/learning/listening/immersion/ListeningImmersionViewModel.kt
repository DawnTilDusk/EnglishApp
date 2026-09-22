package com.example.seedie.ui.screens.learning.listening.immersion

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.seedie.data.remote.ListeningRemoteDataSource
import com.example.seedie.data.remote.SupabaseListeningMaterial
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal fun immersionMaterials(materials: List<SupabaseListeningMaterial>) = materials
    .filter { it.book_id.isNullOrBlank() && it.material_id != "l08-01" &&
        !it.transcript.isNullOrBlank() && it.audio_url?.startsWith("https://") == true }
    .distinctBy { it.material_id }
    .sortedBy { it.sort_order }

internal fun resumePosition(position: Long, duration: Long): Long = when {
    position < 0 -> 0
    duration > 0 && position >= (duration - 500).coerceAtLeast(0) -> 0
    else -> position
}

data class ImmersionUiState(
    val loading: Boolean = true,
    val materials: List<SupabaseListeningMaterial> = emptyList(),
    val currentId: String? = null,
    val isPlaying: Boolean = false,
    val buffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val speed: Float = 1f,
    val repeatOne: Boolean = false,
    val hasHistory: Boolean = false,
    val loadError: String? = null,
    val playbackError: String? = null
) {
    val current get() = materials.firstOrNull { it.material_id == currentId }
}

@HiltViewModel
class ListeningImmersionViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val remote: ListeningRemoteDataSource
) : ViewModel() {
    private val preferences = context.getSharedPreferences("listening_immersion", Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(ImmersionUiState(
        speed = preferences.getFloat("speed", 1f).coerceIn(.75f, 1.5f),
        repeatOne = preferences.getBoolean("repeat", false)
    ))
    val uiState = _uiState.asStateFlow()
    private val player = ExoPlayer.Builder(context).build().apply {
        setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(), true)
        setHandleAudioBecomingNoisy(true)
        setPlaybackSpeed(_uiState.value.speed)
        repeatMode = if (_uiState.value.repeatOne) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    init {
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                refreshPlayback()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                    _uiState.value.currentId?.let { preferences.edit().putLong("position:$it", 0).apply() }
                }
                _uiState.update { it.copy(currentId = mediaItem?.mediaId, playbackError = null) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) saveProgress()
            }

            override fun onPlayerError(error: PlaybackException) {
                _uiState.update { it.copy(playbackError = "音频加载失败，请检查网络后重试。", buffering = false) }
                saveProgress()
            }
        })
        reload()
        viewModelScope.launch {
            var ticks = 0
            while (isActive) {
                delay(500)
                refreshPlayback()
                if (++ticks % 10 == 0 && player.isPlaying) saveProgress()
            }
        }
    }

    fun reload() {
        if (_uiState.value.materials.isNotEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, loadError = null) }
            try {
                val materials = immersionMaterials(remote.fetchAllMaterials())
                val lastId = preferences.getString("last_id", null)
                val index = materials.indexOfFirst { it.material_id == lastId }.coerceAtLeast(0)
                val current = materials.getOrNull(index)
                val position = current?.let { preferences.getLong("position:${it.material_id}", 0).coerceAtLeast(0) } ?: 0
                _uiState.update { it.copy(loading = false, materials = materials,
                    currentId = current?.material_id, positionMs = position,
                    hasHistory = current != null && current.material_id == lastId) }
                if (materials.isNotEmpty()) {
                    player.setMediaItems(materials.map {
                        MediaItem.Builder().setMediaId(it.material_id).setUri(requireNotNull(it.audio_url)).build()
                    }, index, position)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update { it.copy(loading = false, loadError = "内容加载失败，请检查网络后重试。") }
            }
        }
    }

    fun open(materialId: String) {
        val index = _uiState.value.materials.indexOfFirst { it.material_id == materialId }
        if (index < 0) return
        if (player.currentMediaItem?.mediaId != materialId) {
            saveProgress()
            player.seekTo(index, preferences.getLong("position:$materialId", 0).coerceAtLeast(0))
        }
        play()
    }

    private fun play() {
        if (player.mediaItemCount == 0) return
        _uiState.update { it.copy(playbackError = null, hasHistory = true) }
        val position = resumePosition(player.currentPosition, player.duration)
        if (position != player.currentPosition || player.playbackState == Player.STATE_ENDED) player.seekTo(position)
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        player.play()
        saveProgress()
    }

    fun togglePlayback() {
        if (player.playWhenReady && player.playerError == null) pause() else play()
    }

    fun pause() {
        player.pause()
        saveProgress()
        refreshPlayback()
    }

    fun seek(position: Long) {
        if (player.duration <= 0) return
        player.seekTo(position.coerceIn(0, player.duration))
        refreshPlayback()
        saveProgress()
    }

    fun skip(seconds: Int) = seek(player.currentPosition + seconds * 1000L)

    fun next() {
        if (!player.hasNextMediaItem()) return
        saveProgress()
        player.seekToNextMediaItem()
        play()
    }

    fun previous() {
        if (!player.hasPreviousMediaItem()) { seek(0); return }
        saveProgress()
        player.seekToPreviousMediaItem()
        play()
    }

    fun cycleSpeed() {
        val speeds = listOf(.75f, 1f, 1.25f, 1.5f)
        val speed = speeds[(speeds.indexOf(_uiState.value.speed) + 1) % speeds.size]
        player.setPlaybackSpeed(speed)
        preferences.edit().putFloat("speed", speed).apply()
        _uiState.update { it.copy(speed = speed) }
    }

    fun toggleRepeat() {
        val repeat = !_uiState.value.repeatOne
        player.repeatMode = if (repeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        preferences.edit().putBoolean("repeat", repeat).apply()
        _uiState.update { it.copy(repeatOne = repeat) }
    }

    private fun refreshPlayback() {
        _uiState.update { it.copy(
            currentId = player.currentMediaItem?.mediaId ?: it.currentId,
            isPlaying = player.isPlaying,
            buffering = player.playbackState == Player.STATE_BUFFERING,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.coerceAtLeast(0)
        ) }
    }

    private fun saveProgress() {
        val id = player.currentMediaItem?.mediaId ?: return
        // Merely loading the catalog must not create a listening history entry.
        if (!_uiState.value.hasHistory) return
        val position = if (player.playbackState == Player.STATE_ENDED) 0 else player.currentPosition.coerceAtLeast(0)
        preferences.edit().putString("last_id", id).putLong("position:$id", position).apply()
    }

    override fun onCleared() {
        saveProgress()
        player.release()
        super.onCleared()
    }
}

package com.example.seedie.ui.screens.learning.listening.textbook

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.seedie.data.audio.WordAudioPlayer
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningTextbookAudioRoute(
    onNavigateBack: () -> Unit,
    viewModel: ListeningTextbookAudioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val audioPlayer = remember { WordAudioPlayer(context.applicationContext) }
    val isPlaying by audioPlayer.isPlaying.collectAsState()
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }
    var playbackMessage by remember { mutableStateOf<String?>(null) }
    var showChinese by remember { mutableStateOf(false) }

    fun speakFallback(text: String) {
        if (isTtsReady) {
            audioPlayer.stop()
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "textbook-listening")
        } else {
            playbackMessage = "设备语音服务尚未准备好，请稍后重试。"
        }
    }

    fun playMaterial() {
        val material = uiState.material ?: return
        val fallbackText = material.transcript
            ?.takeIf { it.isNotBlank() }
            ?: material.promptText
                ?.takeIf { it.isNotBlank() }
            ?: material.title

        val audioUrl = material.audioUrl?.takeIf { it.isNotBlank() }
        if (audioUrl == null) {
            playbackMessage = "正在使用设备语音朗读该听力材料。"
            speakFallback(fallbackText)
        } else {
            playbackMessage = null
            textToSpeech?.stop()
            audioPlayer.playUrl(audioUrl) {
                playbackMessage = "音频加载失败，正在使用设备语音朗读。"
                speakFallback(fallbackText)
            }
        }
    }

    DisposableEffect(Unit) {
        var speaker: TextToSpeech? = null
        speaker = TextToSpeech(context.applicationContext) { status ->
            isTtsReady = status == TextToSpeech.SUCCESS
            if (isTtsReady) {
                speaker?.language = Locale.US
            }
        }
        textToSpeech = speaker
        onDispose {
            audioPlayer.release()
            speaker?.stop()
            speaker?.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("教材听力") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            uiState.material == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.errorMessage ?: "未找到这条教材音频。",
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = viewModel::reload) { Text("重试") }
                }
            }

            else -> {
                val material = requireNotNull(uiState.material)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = material.titleZh ?: material.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    material.titleZh?.takeIf { it != material.title }?.let {
                        Text(
                            text = material.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    material.promptText?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                    Button(
                        onClick = {
                            if (isPlaying) {
                                audioPlayer.stop()
                                playbackMessage = null
                            } else {
                                playMaterial()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.56f)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Text(
                            text = if (isPlaying) "停止播放" else "播放音频",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    OutlinedButton(
                        onClick = { showChinese = !showChinese },
                        modifier = Modifier.fillMaxWidth(0.56f)
                    ) {
                        Text(if (showChinese) "收起中文对照" else "中文对照")
                    }
                    playbackMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (showChinese) {
                        TranscriptCard(
                            title = "中文对照",
                            content = material.transcriptZh
                                ?: material.titleZh
                                ?: "这条教材音频暂未提供中文对照。"
                        )
                    }
                    material.transcript?.takeIf { it.isNotBlank() }?.let {
                        TranscriptCard(title = "听力原文", content = it)
                    }
                }
            }
        }
    }
}

@Composable
private fun TranscriptCard(title: String, content: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

package com.example.seedie.ui.screens.learning.listening

import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.example.seedie.BuildConfig
import com.example.seedie.data.audio.WordAudioPlayer
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.ui.components.PracticeOptionCard
import com.example.seedie.ui.screens.learning.practice.AnswerStatus
import com.example.seedie.ui.theme.gardenShadow
import java.util.Locale

@Composable
fun ListeningPracticeRoute(
    onFinishSession: (StudyResult) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ListeningPracticeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val audioPlayer = remember { WordAudioPlayer(context.applicationContext) }
    var speaker by remember { mutableStateOf<TextToSpeech?>(null) }
    var speakerReady by remember { mutableStateOf(false) }
    var pendingSpeech by remember { mutableStateOf<String?>(null) }

    fun logAudioDebug(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(AUDIO_LOG_TAG, message)
        }
    }

    fun speakFallback(english: String) {
        if (speakerReady) {
            audioPlayer.stop()
            logAudioDebug("TTS speak: $english")
            speaker?.speak(
                english,
                TextToSpeech.QUEUE_FLUSH,
                null,
                english
            )
            pendingSpeech = null
        } else {
            logAudioDebug("TTS not ready, queue pending: $english")
            pendingSpeech = english
        }
    }

    fun playWordAudio(rawResId: Int, fallbackEnglish: String) {
        speaker?.stop()
        if (rawResId != 0) {
            logAudioDebug("playWordAudio MediaPlayer rawResId=$rawResId fallback=$fallbackEnglish")
            audioPlayer.playRaw(rawResId) {
                logAudioDebug("MediaPlayer failed, fallback to TTS: $fallbackEnglish")
                speakFallback(fallbackEnglish)
            }
        } else {
            logAudioDebug("playWordAudio no raw resource, TTS: $fallbackEnglish")
            speakFallback(fallbackEnglish)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    DisposableEffect(Unit) {
        var textToSpeech: TextToSpeech? = null
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                speakerReady = true
                textToSpeech?.language = Locale.US
            } else {
                logAudioDebug("TextToSpeech init failed status=$status")
            }
        }
        speaker = textToSpeech
        onDispose {
            audioPlayer.release()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            speaker = null
            speakerReady = false
            pendingSpeech = null
        }
    }

    LaunchedEffect(speakerReady, pendingSpeech) {
        val queued = pendingSpeech
        if (speakerReady && queued != null) {
            logAudioDebug("Playing pending TTS: $queued")
            speakFallback(queued)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.studyResults.collect { result ->
            onFinishSession(result)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.playAudioEvents.collect { event ->
            playWordAudio(event.rawResId, event.fallbackEnglish)
        }
    }

    ListeningPracticeScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onBackClick = viewModel::onBackClick,
        onConfirmExit = viewModel::onConfirmExit,
        onDismissExitDialog = viewModel::onDismissExitDialog,
        onOptionSelected = viewModel::onOptionSelected,
        onSubmitAnswer = viewModel::onSubmitAnswer,
        onNextQuestion = viewModel::onNextQuestion,
        onReplayAudio = viewModel::onReplayAudio,
        onRetryLoad = viewModel::onRetryLoad,
        onFinishSession = viewModel::onFinishSession
    )
}

@Composable
private fun ListeningPracticeScreen(
    uiState: ListeningPracticeUiState,
    onNavigateBack: () -> Unit,
    onBackClick: () -> Unit,
    onConfirmExit: () -> Unit,
    onDismissExitDialog: () -> Unit,
    onOptionSelected: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onReplayAudio: () -> Unit,
    onRetryLoad: () -> Unit,
    onFinishSession: () -> Unit
) {
    if (uiState.showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                Button(onClick = onConfirmExit) {
                    Text("确认退出")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissExitDialog) {
                    Text("继续练习")
                }
            },
            title = { Text("退出后将结束本次听力训练") },
            text = { Text("已完成的作答会被记录，但你将离开当前练习。") }
        )
    }

    when (uiState.stage) {
        ListeningPracticeStage.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        ListeningPracticeStage.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = uiState.errorMessage ?: "加载失败",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onRetryLoad) { Text("重试") }
                    Button(onClick = onNavigateBack) { Text("返回") }
                }
            }
        }

        ListeningPracticeStage.Completed -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("听力训练完成", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "正确 ${uiState.correctCount} / ${uiState.totalCount}，获得 ${uiState.earnedTokens} 代币",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
                )
                Button(onClick = onFinishSession) {
                    Text("完成")
                }
            }
        }

        ListeningPracticeStage.Ready,
        ListeningPracticeStage.AnswerEvaluated -> {
            val question = uiState.currentQuestion ?: return
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .gardenShadow(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable(onClick = onBackClick)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "返回"
                                )
                                Text(
                                    text = "听力训练",
                                    modifier = Modifier.padding(start = 8.dp),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("第 ${uiState.currentIndex + 1} / ${uiState.totalCount} 题")
                                Text("用时 ${uiState.elapsedSeconds}s")
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "听音频，选择正确的英文单词",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            IconButton(
                                onClick = onReplayAudio,
                                modifier = Modifier
                                    .size(88.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "播放单词发音",
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (uiState.stage == ListeningPracticeStage.AnswerEvaluated) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = question.english,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${question.phonetic}  ${question.translation}",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = uiState.feedbackMessage,
                                            color = if (uiState.answerStatus == AnswerStatus.Correct) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.error
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        question.options.forEach { option ->
                            PracticeOptionCard(
                                option = option,
                                selectedOptionId = uiState.selectedOptionId,
                                answerStatus = uiState.answerStatus,
                                interactionEnabled = uiState.stage == ListeningPracticeStage.Ready,
                                feedbackVisible = uiState.stage == ListeningPracticeStage.AnswerEvaluated,
                                onOptionSelected = onOptionSelected
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            when (uiState.stage) {
                                ListeningPracticeStage.Ready -> {
                                    Button(
                                        onClick = onSubmitAnswer,
                                        enabled = uiState.canSubmitAnswer
                                    ) {
                                        Text("提交")
                                    }
                                }

                                ListeningPracticeStage.AnswerEvaluated -> {
                                    Button(onClick = onNextQuestion) {
                                        Text(
                                            if (uiState.currentIndex + 1 >= uiState.totalCount) {
                                                "查看结果"
                                            } else {
                                                "下一题"
                                            }
                                        )
                                    }
                                }

                                else -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val AUDIO_LOG_TAG = "SeedieAudio"

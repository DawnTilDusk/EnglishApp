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
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.example.seedie.ui.screens.garden.GardenExitConfirmDialog
import com.example.seedie.ui.screens.garden.PlantSessionGate
import com.example.seedie.ui.screens.learning.assignments.PracticeAssignmentArgs
import com.example.seedie.ui.screens.learning.catalog.FreePracticeArgs
import com.example.seedie.ui.screens.learning.practice.AnswerStatus
import com.example.seedie.ui.theme.gardenShadow
import java.util.Locale

@Composable
fun ListeningPracticeRoute(
    assignmentArgs: PracticeAssignmentArgs?,
    freeArgs: FreePracticeArgs?,
    onFinishSession: (StudyResult) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ListeningPracticeViewModel = hiltViewModel()
) {
    PlantSessionGate { speciesId ->
        ListeningPracticeRouteBody(
            assignmentArgs = assignmentArgs,
            freeArgs = freeArgs,
            speciesId = speciesId,
            onFinishSession = onFinishSession,
            onNavigateBack = onNavigateBack,
            viewModel = viewModel
        )
    }
}

@Composable
private fun ListeningPracticeRouteBody(
    assignmentArgs: PracticeAssignmentArgs?,
    freeArgs: FreePracticeArgs?,
    speciesId: String,
    onFinishSession: (StudyResult) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ListeningPracticeViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val audioPlayer = remember { WordAudioPlayer(context.applicationContext) }
    var speaker by remember { mutableStateOf<TextToSpeech?>(null) }
    var speakerReady by remember { mutableStateOf(false) }
    var pendingSpeech by remember { mutableStateOf<String?>(null) }
    var audioFeedbackMessage by remember { mutableStateOf<String?>(null) }

    fun logAudioDebug(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(AUDIO_LOG_TAG, message)
        }
    }

    fun speakFallback(text: String) {
        if (speakerReady) {
            audioPlayer.stop()
            logAudioDebug("TTS speak: $text")
            speaker?.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                text
            )
            pendingSpeech = null
        } else {
            logAudioDebug("TTS not ready, queue pending: $text")
            pendingSpeech = text
        }
    }

    fun playWordAudio(audioUrl: String?, fallbackText: String) {
        speaker?.stop()
        val resolvedUrl = audioUrl?.takeIf { it.isNotBlank() }
        if (resolvedUrl != null) {
            logAudioDebug("playWordAudio ExoPlayer url=$resolvedUrl")
            audioFeedbackMessage = null
            audioPlayer.playUrl(resolvedUrl) {
                logAudioDebug("playWordAudio remote audio failed, fallback to TTS")
                audioFeedbackMessage = "远端音频播放失败，已改用文本播报，请稍后重试。"
                speakFallback(fallbackText)
            }
        } else {
            logAudioDebug("playWordAudio no remote audio, TTS: $fallbackText")
            audioFeedbackMessage = "远端音频当前不可用，已改用文本播报。"
            speakFallback(fallbackText)
        }
    }

    LaunchedEffect(assignmentArgs?.submissionId, assignmentArgs?.mode, freeArgs?.itemRef, speciesId) {
        viewModel.setSelectedSpeciesId(speciesId)
        when {
            assignmentArgs != null -> viewModel.initializeAssignment(assignmentArgs)
            freeArgs != null -> viewModel.initializeFree(freeArgs)
        }
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
            playWordAudio(event.audioUrl, event.fallbackText)
        }
    }

    ListeningPracticeScreen(
        uiState = uiState,
        audioFeedbackMessage = audioFeedbackMessage,
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
    audioFeedbackMessage: String?,
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
        if (uiState.isReviewMode) {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {
                    Button(onClick = onConfirmExit) { Text("确认退出") }
                },
                dismissButton = {
                    OutlinedButton(onClick = onDismissExitDialog) { Text("继续练习") }
                },
                title = { Text("退出回顾？") },
                text = { Text("进度不会改变。") }
            )
        } else {
            GardenExitConfirmDialog(
                answeredQuestionCount = uiState.correctCount + uiState.wrongCount,
                withinAbandonGrace = com.example.seedie.domain.usecase.GardenForestRules
                    .isWithinAbandonGrace(uiState.sessionOpenedAtMillis),
                onConfirmExit = onConfirmExit,
                onContinue = onDismissExitDialog
            )
        }
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
                Text(
                    if (uiState.isReviewMode) "作业回顾完成" else "作业已提交",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "共完成 ${uiState.totalQuestionCount} 题，正确 ${uiState.correctCount}，错误 ${uiState.wrongCount}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
                if (!uiState.isReviewMode) {
                    Text(
                        text = "本局将种入你的花园 · 用时 ${uiState.elapsedSeconds}s",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                    )
                }
                Button(
                    onClick = onFinishSession,
                    modifier = Modifier.padding(top = if (uiState.isReviewMode) 24.dp else 0.dp)
                ) {
                    Text("完成")
                }
            }
        }

        ListeningPracticeStage.Ready,
        ListeningPracticeStage.AnswerEvaluated -> {
            val material = uiState.currentMaterial ?: return
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
                        .weight(1f)
                        .gardenShadow(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(20.dp),
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
                                if (uiState.totalMaterialCount > 1) {
                                    Text("材料 ${uiState.currentMaterialIndex + 1} / ${uiState.totalMaterialCount}")
                                }
                                Text(
                                    "材料内第 ${uiState.currentQuestionIndexInMaterial + 1} / ${uiState.currentQuestionCountInMaterial} 题"
                                )
                                Text("总进度 ${uiState.currentQuestionOrdinal} / ${uiState.totalQuestionCount}")
                                Text("用时 ${uiState.elapsedSeconds}s")
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = material.titleZh ?: material.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                if (!material.promptText.isNullOrBlank()) {
                                    Text(
                                        text = material.promptText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
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
                                        contentDescription = "播放听力材料",
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "播放听力材料",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!audioFeedbackMessage.isNullOrBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Text(
                                            text = audioFeedbackMessage,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                Text(
                                    text = question.stem,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
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
                                                text = uiState.feedbackMessage,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            if (!question.explanation.isNullOrBlank()) {
                                                Text(
                                                    text = question.explanation,
                                                    style = MaterialTheme.typography.bodyMedium,
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
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
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
                            }
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
                                    val isLast =
                                        uiState.currentQuestionOrdinal >= uiState.totalQuestionCount
                                    Button(onClick = onNextQuestion) {
                                        Text(
                                            when {
                                                isLast && uiState.isReviewMode -> "完成回顾"
                                                isLast -> "提交作业"
                                                uiState.currentQuestionIndexInMaterial + 1 >=
                                                    uiState.currentQuestionCountInMaterial -> "下一篇材料"
                                                else -> "下一题"
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
